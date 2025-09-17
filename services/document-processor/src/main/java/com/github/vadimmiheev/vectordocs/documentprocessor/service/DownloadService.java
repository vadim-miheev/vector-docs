package com.github.vadimmiheev.vectordocs.documentprocessor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class DownloadService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public byte[] download(URI uri, String userId) throws IOException, InterruptedException {
        log.debug("Downloading file from {}", uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("X-User-Id", userId)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        log.debug("Downloaded file from {}. Status: {}", uri, response.statusCode());
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return response.body();
        }
        throw new IOException("Failed to download file. HTTP status: " + status + " from " + uri);
    }
}
