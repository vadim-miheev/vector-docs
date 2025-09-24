package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@AllArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DownloadService downloadService;
    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;

    public void process(DocumentUploadedEvent event) {
        try {
            byte[] bytes = downloadService.download(event.getDownloadUrl(), event.getUserId());
            ArrayList<String> pages = textExtractionService.extractText(bytes, event.getContentType(), event.getName());
            // Generate and persist embeddings
            embeddingService.generateAndSaveEmbeddings(event, pages);
            // Log result
            String preview = pages.getFirst();
            log.info("Processed document id={} name='{}' size={} bytes. Extracted {} chars. Preview: {}",
                    event.getId(), event.getName(), event.getSize(), preview.length(), preview.replaceAll("\n", " "));
        } catch (Exception e) {
            log.error("Failed to process uploaded document id={} name='{}' due to: {}", event.getId(), event.getName(), e.getMessage(), e);
        }
    }
}
