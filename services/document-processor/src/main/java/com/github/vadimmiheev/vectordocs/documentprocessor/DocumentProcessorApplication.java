package com.github.vadimmiheev.vectordocs.documentprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class DocumentProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocumentProcessorApplication.class, args);
    }
}
