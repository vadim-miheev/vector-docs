package com.github.vadimmiheev.vectordocs.answergenerator.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Test configuration for WireMock HTTP API mocking.
 * Provides a WireMock server and a RestTemplate configured to use it.
 */
@TestConfiguration
public class WireMockTestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(0); // random port
        // Stubs will be configured in tests using WireMockUtils or directly
        return wireMockServer;
    }

    @Bean
    public RestTemplate wireMockRestTemplate(WireMockServer wireMockServer) {
        // Create RestTemplate that points to WireMock server
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://localhost:" + wireMockServer.port();
        // Use a custom request factory to redirect all requests to WireMock
        // In actual tests, you would configure your service to use this base URL
        return restTemplate;
    }
}