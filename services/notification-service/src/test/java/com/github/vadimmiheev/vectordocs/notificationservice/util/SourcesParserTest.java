package com.github.vadimmiheev.vectordocs.notificationservice.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourcesParserTest {

    private static final String REQUEST_ID = "test-request-123";

    @BeforeEach
    void setUp() {
        SourcesParser.clear();
    }

    @AfterEach
    void tearDown() {
        SourcesParser.clear();
    }

    @Test
    void processNextToken_withoutTags_shouldReturnSameToken() {
        // Given
        String requestId = "test-no-tags";
        String token = "Hello world";

        // When
        String result = SourcesParser.processNextToken(requestId, token);

        // Then
        assertEquals(token, result);
        assertFalse(SourcesParser.isSourcesReady(requestId));
    }

    @Test
    void processNextToken_withBeginTag_shouldStartBuffering() {
        // Given
        String token = "Hello <BEGIN_SOURCES>";

        // When
        String result = SourcesParser.processNextToken(REQUEST_ID, token);

        // Then
        assertEquals("Hello ", result);
        assertFalse(SourcesParser.isSourcesReady(REQUEST_ID));
    }

    @Test
    void processNextToken_withCompleteTags_shouldExtractSources() {
        // Given
        String token1 = "Hello <BEGIN_SOURCES>";
        String token2 = "file123/document.pdf/1";
        String token3 = "<END_SOURCES> world";

        // When
        String result1 = SourcesParser.processNextToken(REQUEST_ID, token1);
        String result2 = SourcesParser.processNextToken(REQUEST_ID, token2);
        String result3 = SourcesParser.processNextToken(REQUEST_ID, token3);

        // Then
        assertEquals("Hello ", result1);
        assertEquals("", result2); // Buffered
        assertEquals(" world", result3);
        assertTrue(SourcesParser.isSourcesReady(REQUEST_ID));
    }

    @Test
    void processNextToken_withTagsSplitAcrossTokens_shouldHandleCorrectly() {
        // Given
        String requestId = "split-tags-request";
        String token1 = "Hello <BEGIN_";
        String token2 = "SOURCES>file123";
        String token3 = "/document.pdf/1<END";
        String token4 = "_SOURCES> world";

        // When
        String result1 = SourcesParser.processNextToken(requestId, token1);
        String result2 = SourcesParser.processNextToken(requestId, token2);
        String result3 = SourcesParser.processNextToken(requestId, token3);
        String result4 = SourcesParser.processNextToken(requestId, token4);

        // Then
        assertEquals("Hello ", result1);
        assertEquals("", result2); // Buffered
        assertEquals("", result3); // Buffered
        assertEquals(" world", result4);
        assertTrue(SourcesParser.isSourcesReady(requestId));
    }

    @Test
    void getSources_withValidSources_shouldParseCorrectly() {
        // Given
        String requestId = REQUEST_ID + "-sources";
        String sourcesText = "file123/document.pdf/1\nfile456/report.docx/3";
        // Simulate streaming tokens: part before BEGIN_TAG, sources, part after END_TAG
        String result1 = SourcesParser.processNextToken(requestId, "Hello <BEGIN_SOURCES>");
        String result2 = SourcesParser.processNextToken(requestId, sourcesText);
        String result3 = SourcesParser.processNextToken(requestId, "<END_SOURCES> world");
        System.out.println("DEBUG: result1 = " + result1 + ", result2 = " + result2 + ", result3 = " + result3);
        System.out.println("DEBUG: isSourcesReady = " + SourcesParser.isSourcesReady(requestId));

        // When
        ArrayList<Map<String, String>> sources = SourcesParser.getSources(requestId);
        System.out.println("DEBUG: sources size = " + sources.size());
        sources.forEach(s -> System.out.println("DEBUG: source = " + s));

        // Then
        assertEquals(2, sources.size());

        Map<String, String> source1 = sources.get(0);
        assertEquals("file123", source1.get("id"));
        assertEquals("document.pdf", source1.get("name"));
        assertEquals("1", source1.get("page"));

        Map<String, String> source2 = sources.get(1);
        assertEquals("file456", source2.get("id"));
        assertEquals("report.docx", source2.get("name"));
        assertEquals("3", source2.get("page"));
    }

    @Test
    void getSources_withEmptySources_shouldReturnEmptyList() {
        // Given
        String requestId = "empty-sources-request";
        SourcesParser.processNextToken(requestId, "Hello <BEGIN_SOURCES><END_SOURCES> world");

        // When
        ArrayList<Map<String, String>> sources = SourcesParser.getSources(requestId);

        // Then
        assertNotNull(sources);
        assertTrue(sources.isEmpty());
    }

    @Test
    void getSources_withInvalidFormat_shouldSkipInvalidLines() {
        // Given
        String requestId = REQUEST_ID + "-invalid";
        String sourcesText = "file123/document.pdf/1\ninvalid-format\nfile456/report.docx/3\nanother-invalid";
        SourcesParser.processNextToken(requestId, "Hello <BEGIN_SOURCES>");
        SourcesParser.processNextToken(requestId, sourcesText);
        SourcesParser.processNextToken(requestId, "<END_SOURCES> world");

        // When
        ArrayList<Map<String, String>> sources = SourcesParser.getSources(requestId);

        // Then
        assertEquals(2, sources.size()); // Only valid lines

        Map<String, String> source1 = sources.get(0);
        assertEquals("file123", source1.get("id"));
        assertEquals("document.pdf", source1.get("name"));
        assertEquals("1", source1.get("page"));

        Map<String, String> source2 = sources.get(1);
        assertEquals("file456", source2.get("id"));
        assertEquals("report.docx", source2.get("name"));
        assertEquals("3", source2.get("page"));
    }

    @Test
    void getSources_withWindowsLineEndings_shouldParseCorrectly() {
        // Given
        String requestId = REQUEST_ID + "-windows";
        String sourcesText = "file123/document.pdf/1\r\nfile456/report.docx/3";
        SourcesParser.processNextToken(requestId, "Hello <BEGIN_SOURCES>");
        SourcesParser.processNextToken(requestId, sourcesText);
        SourcesParser.processNextToken(requestId, "<END_SOURCES> world");

        // When
        ArrayList<Map<String, String>> sources = SourcesParser.getSources(requestId);

        // Then
        assertEquals(2, sources.size());

        Map<String, String> source1 = sources.get(0);
        assertEquals("file123", source1.get("id"));
        assertEquals("document.pdf", source1.get("name"));
        assertEquals("1", source1.get("page"));

        Map<String, String> source2 = sources.get(1);
        assertEquals("file456", source2.get("id"));
        assertEquals("report.docx", source2.get("name"));
        assertEquals("3", source2.get("page"));
    }

    @Test
    void isSourcesReady_beforeExtraction_shouldReturnFalse() {
        // Given
        String requestId = "not-ready-request";

        // When
        boolean ready = SourcesParser.isSourcesReady(requestId);

        // Then
        assertFalse(ready);
    }

    @Test
    void isSourcesReady_afterExtraction_shouldReturnTrue() {
        // Given
        String requestId = "ready-request";
        SourcesParser.processNextToken(requestId, "Hello <BEGIN_SOURCES>");
        SourcesParser.processNextToken(requestId, "file123/doc.pdf/1");
        SourcesParser.processNextToken(requestId, "<END_SOURCES> world");

        // When
        boolean ready = SourcesParser.isSourcesReady(requestId);

        // Then
        assertTrue(ready);
    }
}