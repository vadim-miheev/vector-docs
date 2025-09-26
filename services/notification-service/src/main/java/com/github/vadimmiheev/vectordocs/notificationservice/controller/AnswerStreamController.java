package com.github.vadimmiheev.vectordocs.notificationservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class AnswerStreamController {

    private static final Logger log = LoggerFactory.getLogger(AnswerStreamController.class);

    @PostMapping(value = "/answers/stream", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void streamAnswer(@RequestParam("userId") String userId, HttpServletRequest request) {
        try (InputStream is = request.getInputStream()) {
            byte[] buffer = new byte[2048];
            int read;
            while ((read = is.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read, StandardCharsets.UTF_8);
                log.info("[answer-stream][userId={}] {}", userId, chunk);
                // TODO: forward chunks to WebSocket/SSE clients if needed in future
            }
            log.info("[answer-stream][userId={}] completed", userId);
        } catch (Exception e) {
            log.error("Failed to consume streamed answer for userId={}", userId, e);
        }
    }
}
