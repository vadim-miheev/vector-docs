package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TextExtractionServiceTest {

    private TextExtractionService textExtractionService;

    @BeforeEach
    void setUp() {
        textExtractionService = new TextExtractionService();
        // Set OCR disabled for tests
        ReflectionTestUtils.setField(textExtractionService, "ocrEnabled", false);
        ReflectionTestUtils.setField(textExtractionService, "ocrLang", "eng");
        ReflectionTestUtils.setField(textExtractionService, "ocrDataPath", "");
        ReflectionTestUtils.setField(textExtractionService, "ocrDpi", 300);
    }

    @Test
    void extractText_textFile_returnsTextContent() throws IOException, TesseractException {
        // Arrange
        byte[] data = "Hello, World!\nThis is a text file.".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";
        String fileName = "test.txt";

        // Act
        ArrayList<String> result = textExtractionService.extractText(data, contentType, fileName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hello, World!\nThis is a text file.", result.getFirst());
    }

    @Test
    void extractText_textFileWithCrLf_normalizesNewlines() throws IOException, TesseractException {
        // Arrange
        byte[] data = "Line 1\r\nLine 2\rLine 3\nLine 4".getBytes(StandardCharsets.UTF_8);
        String contentType = "text/plain";
        String fileName = "test.txt";

        // Act
        ArrayList<String> result = textExtractionService.extractText(data, contentType, fileName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Line 1\nLine 2\nLine 3\nLine 4", result.getFirst());
    }

    @Test
    void extractText_textFileByExtension_returnsTextContent() throws IOException, TesseractException {
        // Arrange
        byte[] data = "Text content".getBytes(StandardCharsets.UTF_8);
        String contentType = null; // No content type
        String fileName = "file.TXT"; // Uppercase extension

        // Act
        ArrayList<String> result = textExtractionService.extractText(data, contentType, fileName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Text content", result.getFirst());
    }

    @Test
    void extractText_unsupportedContentType_throwsIOException() {
        // Arrange
        byte[] data = new byte[]{1, 2, 3};
        String contentType = "image/jpeg";
        String fileName = "photo.jpg";

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () ->
                textExtractionService.extractText(data, contentType, fileName)
        );
        assertTrue(exception.getMessage().contains("Unsupported content type"));
        assertTrue(exception.getMessage().contains("image/jpeg"));
        assertTrue(exception.getMessage().contains("photo.jpg"));
    }

    @Test
    void extractText_nullContentTypeAndUnsupportedExtension_throwsIOException() {
        // Arrange
        byte[] data = new byte[]{1, 2, 3};
        String contentType = null;
        String fileName = "image.jpg";

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () ->
                textExtractionService.extractText(data, contentType, fileName)
        );
        assertTrue(exception.getMessage().contains("Unsupported content type"));
        assertTrue(exception.getMessage().contains("image.jpg"));
    }
}