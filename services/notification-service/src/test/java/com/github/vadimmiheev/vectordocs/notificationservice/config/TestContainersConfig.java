package com.github.vadimmiheev.vectordocs.notificationservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for Testcontainers.
 * Provides PostgreSQL and Kafka containers for integration tests.
 */
@TestConfiguration
public class TestContainersConfig {

    private static final String POSTGRES_IMAGE = "pgvector/pgvector:pg16";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.5.0";

    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
        // Enable pgvector extension
        container.withCommand("postgres", "-c", "shared_preload_libraries=pgvector");
        return container;
    }

    @Bean
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
    }
}