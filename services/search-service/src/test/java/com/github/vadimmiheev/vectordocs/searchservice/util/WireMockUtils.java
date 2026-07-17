package com.github.vadimmiheev.vectordocs.searchservice.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Utilities for WireMock HTTP API mocking in tests.
 */
public class WireMockUtils {

    private WireMockUtils() {
        // Utility class
    }

    /**
     * Creates and starts a WireMock server on a random port.
     */
    public static WireMockServer createAndStartWireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        return wireMockServer;
    }

    /**
     * Stops the WireMock server if it's running.
     */
    public static void stopWireMockServer(WireMockServer wireMockServer) {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    /**
     * Configures a mock JSON response for a POST endpoint.
     */
    public static void setupMockJsonResponse(WireMockServer wireMockServer, String url, String requestBody, String responseBody) {
        wireMockServer.stubFor(post(urlEqualTo(url))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));
    }

    /**
     * Configures a mock JSON response for a POST endpoint with any request body.
     */
    public static void setupMockJsonResponseForAnyRequestBody(WireMockServer wireMockServer, String url, String responseBody) {
        wireMockServer.stubFor(post(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));
    }

    /**
     * Configures a mock JSON response for a GET endpoint.
     */
    public static void setupMockJsonResponseForGet(WireMockServer wireMockServer, String url, String responseBody) {
        wireMockServer.stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));
    }

    /**
     * Configures a mock error response (e.g., 500 Internal Server Error).
     */
    public static void setupMockErrorResponse(WireMockServer wireMockServer, String url, int statusCode) {
        wireMockServer.stubFor(post(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"error\":\"Mock error response\"}")));
    }

    /**
     * Creates a mock embedding API response JSON for a vector of given dimension.
     */
    public static String createMockEmbeddingResponse(float[] vector) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"object\":\"list\",\"data\":[{\"object\":\"embedding\",\"embedding\":[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("],\"index\":0}],\"model\":\"text-embedding-ada-002\",\"usage\":{\"prompt_tokens\":5,\"total_tokens\":5}}");
        return sb.toString();
    }

    /**
     * Creates a mock LLM API response JSON for a chat completion.
     */
    public static String createMockLlmChatResponse(String content) {
        return String.format("{\"id\":\"chatcmpl-123\",\"object\":\"chat.completion\",\"created\":1677652288,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"%s\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":9,\"completion_tokens\":12,\"total_tokens\":21}}", content);
    }

    /**
     * Creates a mock LLM API response for streaming (simplified).
     */
    public static String createMockLlmStreamingResponseChunk(String content, boolean isLast) {
        String finishReason = isLast ? "stop" : null;
        return String.format("data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1677652288,\"model\":\"gpt-3.5-turbo-0613\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"%s\"},\"finish_reason\":%s}]}\n\n", content, finishReason != null ? "\"" + finishReason + "\"" : "null");
    }

    /**
     * Verifies that a request was made to the specified endpoint.
     */
    public static void verifyRequestMade(WireMockServer wireMockServer, String url, int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo(url)));
    }

    /**
     * Resets all WireMock stubs and request journal.
     */
    public static void resetWireMock(WireMockServer wireMockServer) {
        wireMockServer.resetAll();
    }
}