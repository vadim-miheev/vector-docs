package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for document upload flow using EmbeddedKafka and H2 database.
 * Tests the complete cycle: file upload → database storage → Kafka event publishing.
 */
@SpringBootTest(
    classes = com.github.vadimmiheev.vectordocs.storageservice.StorageServiceApplication.class,
    properties = {
        "spring.servlet.multipart.max-file-size=1KB",
        "spring.servlet.multipart.max-request-size=1KB"
    }
)
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9094", "port=9094" })
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "DEMO_USER_ID=999"
})
public class DocumentUploadFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    private final String regularUserId = "test-user-123";
    private final String topic = "documents.uploaded";

    /**
     * Polls for a Kafka record with the specified key within the timeout.
     * @param consumer Kafka consumer
     * @param expectedKey Expected key to match
     * @param timeout Maximum time to wait
     * @return The matching ConsumerRecord, or null if not found
     */
    private ConsumerRecord<String, String> pollForRecordWithKey(Consumer<String, String> consumer,
                                                                 String expectedKey, Duration timeout) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                if (expectedKey.equals(record.key())) {
                    return record;
                }
            }
        }
        return null;
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulPdfDocumentUpload_ShouldStoreInDatabaseAndPublishEvent() throws Exception {
        // Create a PDF file mock
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF test content".getBytes()
        );

        // Perform file upload request and capture response to get document ID
        String responseContent = mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test-document.pdf"))
                .andExpect(jsonPath("$.userId").value(regularUserId))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse response to get document ID
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        String documentId = (String) responseMap.get("id");
        assertNotNull(documentId, "Document ID should not be null");

        // Verify that a DocumentUploadedEvent was published to Kafka
        // Create a test consumer with unique group ID
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            // Give consumer time to join group and get partition assignment
            Thread.sleep(1000);

            // Wait for the message to arrive (timeout in milliseconds)
            ConsumerRecord<String, String> record = pollForRecordWithKey(consumer, documentId, Duration.ofMillis(10000));
            assertNotNull(record, "Should find Kafka record with key: " + documentId);

            // Verify key matches document ID
            assertEquals(documentId, record.key(), "Kafka message key should match document ID");

            // Verify message content
            String messageValue = record.value();
            assertNotNull(messageValue, "Kafka message value should not be null");

            // Parse the JSON message
            Map<String, Object> eventMap = objectMapper.readValue(messageValue, Map.class);
            assertEquals(documentId, eventMap.get("id"), "Event ID should match document ID");
            assertEquals("test-document.pdf", eventMap.get("name"), "Event filename should match");
            assertEquals(regularUserId, eventMap.get("userId"), "Event user ID should match");
            assertEquals(MediaType.APPLICATION_PDF_VALUE, eventMap.get("contentType"), "Event content type should match");
            assertEquals("uploaded", eventMap.get("status"), "Event status should be 'uploaded'");
            assertNotNull(eventMap.get("createdAt"), "Event createdAt should not be null");
            assertNotNull(eventMap.get("downloadUrl"), "Event downloadUrl should not be null");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void textFileUpload_ShouldSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Plain text content".getBytes()
        );

        // Perform file upload request and capture response to get document ID
        String responseContent = mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse response to get document ID
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        String documentId = (String) responseMap.get("id");
        assertNotNull(documentId, "Document ID should not be null");

        // Verify that a DocumentUploadedEvent was published to Kafka
        // Create a test consumer with unique group ID
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            // Give consumer time to join group and get partition assignment
            Thread.sleep(1000);

            // Wait for the message to arrive (timeout in milliseconds)
            ConsumerRecord<String, String> record = pollForRecordWithKey(consumer, documentId, Duration.ofMillis(10000));
            assertNotNull(record, "Should find Kafka record with key: " + documentId);

            // Verify key matches document ID
            assertEquals(documentId, record.key(), "Kafka message key should match document ID");

            // Verify message content
            String messageValue = record.value();
            assertNotNull(messageValue, "Kafka message value should not be null");

            // Parse the JSON message
            Map<String, Object> eventMap = objectMapper.readValue(messageValue, Map.class);
            assertEquals(documentId, eventMap.get("id"), "Event ID should match document ID");
            assertEquals("test.txt", eventMap.get("name"), "Event filename should match");
            assertEquals(regularUserId, eventMap.get("userId"), "Event user ID should match");
            assertEquals(MediaType.TEXT_PLAIN_VALUE, eventMap.get("contentType"), "Event content type should match");
            assertEquals("uploaded", eventMap.get("status"), "Event status should be 'uploaded'");
            assertNotNull(eventMap.get("createdAt"), "Event createdAt should not be null");
            assertNotNull(eventMap.get("downloadUrl"), "Event downloadUrl should not be null");
        }
    }

    @Test
    void uploadDocument_WithoutUserIdHeader_ShouldReturnInternalServerError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // Perform upload without X-User-Id header
        // MissingRequestHeaderException is caught by generic Exception handler -> 500
        mockMvc.perform(multipart("/documents")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void uploadDocument_WithUnsupportedFileType_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.jpg",
                "image/jpeg",
                "JPEG content".getBytes()
        );

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only PDF and TXT files are allowed"));
    }

    @Test
    void uploadDocument_ByDemoUser_ShouldReturnForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        String demoUserId = "999"; // matches DEMO_USER_ID property

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header("X-User-Id", demoUserId))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Demo user is not allowed to upload documents")));
    }

    @Test
    void uploadDocument_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("file is required"));
    }

    @Test
    void uploadDocument_WithoutFilePart_ShouldReturnInternalServerError() throws Exception {
        // Perform multipart request without file part
        // MissingServletRequestPartException is caught by generic Exception handler -> 500
        mockMvc.perform(multipart("/documents")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }
}