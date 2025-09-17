package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import com.github.vadimmiheev.vectordocs.documentprocessor.dto.DocumentUploadedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DownloadService downloadService;
    private final TextExtractionService textExtractionService;

    public void process(DocumentUploadedEvent event) {
        try {
            byte[] bytes = downloadService.download(event.getDownloadUrl(), event.getUserId());
            String text = textExtractionService.extractText(bytes, event.getContentType(), event.getName());
            // For now, simply log the result length and a preview.
            String preview = text.length() > 500 ? text.substring(0, 500) + "..." : text;
            log.info("Processed document id={} name='{}' size={} bytes. Extracted {} chars. Preview: {}",
                    event.getId(), event.getName(), event.getSize(), text.length(), preview.replaceAll("\n", " "));
        } catch (Exception e) {
            log.error("Failed to process uploaded document id={} name='{}' due to: {}", event.getId(), event.getName(), e.getMessage(), e);
        }
    }
}
