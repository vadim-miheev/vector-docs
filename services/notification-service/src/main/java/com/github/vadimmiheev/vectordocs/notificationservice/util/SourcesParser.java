package com.github.vadimmiheev.vectordocs.notificationservice.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SourcesParser {
    private static final String BEGIN_TAG = "<BEGIN_SOURCES>";
    private static final String END_TAG = "<END_SOURCES>";
    private static final ConcurrentHashMap<String, RequestMeta> REQUESTS_META = new ConcurrentHashMap<>();

    public static String processNextToken(String requestId, String token) {
        RequestMeta meta = REQUESTS_META.computeIfAbsent(requestId, _ -> new RequestMeta());
        return meta.processToken(token);
    }

    public static boolean isSourcesReady(String requestId) {
        return REQUESTS_META.computeIfAbsent(requestId, _ -> new RequestMeta()).isSourcesReady();
    }

    public static ArrayList<Map<String, String>> getSources(String requestId) {
        RequestMeta meta = REQUESTS_META.get(requestId);
        if (meta == null || meta.sources == null || meta.sources.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Map<String, String>> sourcesList = new ArrayList<>();
        // lines can be separated by \n or \r\n, remove empty ones
        String[] lines = meta.sources.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("/+");
            if (parts.length == 3) {
                String fileUuid = parts[0];
                String fileName = parts[1];
                String pageNumber = parts[2];
                sourcesList.add(Map.of(
                        "id", fileUuid,
                        "name", fileName,
                        "page", pageNumber
                ));
            }
        }
        return sourcesList;
    }

    /**
     * Clears all stored request metadata. For testing purposes only.
     */
    public static void clear() {
        REQUESTS_META.clear();
    }


    @Getter
    @Setter
    private static class RequestMeta {
        private StringBuffer buffer = new StringBuffer();
        private boolean sourcesReady = false;
        private String sources = "";


        public String processToken(String token) {
            String bufferValue = buffer.toString();
            if (bufferValue.isEmpty()) {
                if (token.contains("<")) {
                    String tagPart = token.substring(token.indexOf("<"));
                    if (BEGIN_TAG.contains(tagPart) || tagPart.contains(BEGIN_TAG)) {
                        buffer.append(tagPart);
                        return token.substring(0, token.indexOf("<"));
                    }
                }
                return token;
            } else {
                String result = bufferValue + token;
                if (BEGIN_TAG.contains(result)) {
                    buffer.append(token);
                    return "";
                } else if (result.contains(BEGIN_TAG)) {
                    String beforeTags = "";
                    if (result.indexOf(BEGIN_TAG) > 0) {
                        result = result.substring(result.indexOf(BEGIN_TAG));
                        beforeTags = result.substring(0, result.indexOf(BEGIN_TAG));
                    }
                    if (result.contains(END_TAG)) {
                        buffer = new StringBuffer();
                        String afterTags = extractSources(result);
                        return beforeTags + afterTags;
                    }

                    buffer = new StringBuffer(result);
                    return beforeTags;
                } else {
                    buffer = new StringBuffer();
                    return result;
                }
            }
        }

        private String extractSources(String text) {
            int beginIndex = text.indexOf(BEGIN_TAG);
            int endIndex = text.indexOf(END_TAG);

            if (beginIndex != -1 && endIndex != -1 && beginIndex < endIndex) {
                int start = beginIndex + BEGIN_TAG.length();
                // cut off spaces and hyphens, if any
                sources = text.substring(start, endIndex).trim();
                sourcesReady = true;
                // return a string without tags and content between them
                return text.substring(0, beginIndex) + text.substring(endIndex + END_TAG.length());
            }
            return text;
        }
    }
}
