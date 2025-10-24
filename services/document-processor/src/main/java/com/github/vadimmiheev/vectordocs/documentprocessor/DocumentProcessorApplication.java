package com.github.vadimmiheev.vectordocs.documentprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@SpringBootApplication
@EnableScheduling
public class DocumentProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentProcessorApplication.class, args);
    }
}
