package com.github.vadimmiheev.vectordocs.documentprocessor.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    private TextChunker() {}

    public static List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        if (chunkSize <= 0) {
            chunks.add(text);
            return chunks;
        }
        if (overlap < 0) overlap = 0;
        if (overlap >= chunkSize) overlap = Math.max(0, chunkSize - 1);

        int start = 0;
        int len = text.length();
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            if (end >= len) break;
            start = end - overlap;
        }
        return chunks;
    }
}
