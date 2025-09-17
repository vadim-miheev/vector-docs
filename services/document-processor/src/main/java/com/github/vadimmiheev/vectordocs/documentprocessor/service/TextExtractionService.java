package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TextExtractionService {

    public String extractText(byte[] data, String contentType, String fileName) throws IOException {
        String type = contentType != null ? contentType.toLowerCase() : null;
        String lowerName = fileName != null ? fileName.toLowerCase() : "";

        if ((type != null && type.contains("pdf")) || lowerName.endsWith(".pdf")) {
            return extractFromPdf(data);
        }
        if ((type != null && (type.contains("text/plain") || type.startsWith("text/"))) || lowerName.endsWith(".txt")) {
            String text = new String(data, StandardCharsets.UTF_8);
            // CRLF & CR → LF
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            return text;
        }
        throw new IOException("Unsupported content type: " + contentType + " for file " + fileName);
    }

    private String extractFromPdf(byte[] data) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(data))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            return text != null ? text : "";
        }
    }
}
