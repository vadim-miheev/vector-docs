package com.github.vadimmiheev.vectordocs.searchservice.util;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Utilities for Kafka testing with Testcontainers.
 */
public class KafkaTestUtils {

    private KafkaTestUtils() {
        // Utility class
    }

    /**
     * Creates a Kafka producer for testing.
     */
    public static <K, V> Producer<K, V> createProducer(KafkaContainer kafkaContainer,
                                                       Serializer<K> keySerializer,
                                                       Serializer<V> valueSerializer) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new KafkaProducer<>(props, keySerializer, valueSerializer);
    }

    /**
     * Creates a Kafka consumer for testing.
     */
    public static <K, V> Consumer<K, V> createConsumer(KafkaContainer kafkaContainer,
                                                       Deserializer<K> keyDeserializer,
                                                       Deserializer<V> valueDeserializer,
                                                       String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");

        return new KafkaConsumer<>(props, keyDeserializer, valueDeserializer);
    }

    /**
     * Creates a simple string producer for testing.
     */
    public static Producer<String, String> createStringProducer(KafkaContainer kafkaContainer) {
        return createProducer(kafkaContainer, new StringSerializer(), new StringSerializer());
    }

    /**
     * Creates a simple string consumer for testing.
     */
    public static Consumer<String, String> createStringConsumer(KafkaContainer kafkaContainer, String groupId) {
        return createConsumer(kafkaContainer, new StringDeserializer(), new StringDeserializer(), groupId);
    }

    /**
     * Sends a message to a Kafka topic and returns immediately.
     */
    public static <K, V> void sendMessage(Producer<K, V> producer, String topic, K key, V value) {
        ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
        producer.send(record);
        producer.flush();
    }

    /**
     * Sends a message to a Kafka topic and waits for it to be sent.
     */
    public static <K, V> void sendMessageAndWait(Producer<K, V> producer, String topic, K key, V value) {
        sendMessage(producer, topic, key, value);
        // Small delay to ensure message is sent
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Consumes messages from a topic until a condition is met or timeout occurs.
     */
    public static <K, V> List<ConsumerRecord<K, V>> consumeMessages(Consumer<K, V> consumer,
                                                                    String topic,
                                                                    Predicate<ConsumerRecord<K, V>> condition,
                                                                    long timeoutMs) {
        consumer.subscribe(Collections.singletonList(topic));
        List<ConsumerRecord<K, V>> matchingRecords = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<K, V> record : records) {
                if (condition.test(record)) {
                    matchingRecords.add(record);
                }
            }
            if (!matchingRecords.isEmpty()) {
                break;
            }
        }
        return matchingRecords;
    }

    /**
     * Waits for a message matching the condition to appear in the topic.
     */
    public static <K, V> ConsumerRecord<K, V> waitForMessage(Consumer<K, V> consumer,
                                                             String topic,
                                                             Predicate<ConsumerRecord<K, V>> condition,
                                                             long timeoutMs) {
        List<ConsumerRecord<K, V>> records = consumeMessages(consumer, topic, condition, timeoutMs);
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * Waits for a message with specific key and value (as strings) to appear.
     */
    public static ConsumerRecord<String, String> waitForStringMessage(KafkaContainer kafkaContainer,
                                                                      String topic,
                                                                      String expectedKey,
                                                                      String expectedValue,
                                                                      long timeoutMs) {
        try (Consumer<String, String> consumer = createStringConsumer(kafkaContainer, "test-group-wait")) {
            return waitForMessage(consumer, topic, record ->
                    record.key().equals(expectedKey) && record.value().equals(expectedValue), timeoutMs);
        }
    }

    /**
     * Creates a predicate that matches any record.
     */
    public static <K, V> Predicate<ConsumerRecord<K, V>> anyRecord() {
        return record -> true;
    }

    /**
     * Uses Awaitility to wait until a condition is met for Kafka messages.
     */
    public static <K, V> void awaitUntilMessageAppears(Consumer<K, V> consumer,
                                                       String topic,
                                                       Predicate<ConsumerRecord<K, V>> condition,
                                                       long timeoutSeconds) {
        Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    consumer.subscribe(Collections.singletonList(topic));
                    ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<K, V> record : records) {
                        if (condition.test(record)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}