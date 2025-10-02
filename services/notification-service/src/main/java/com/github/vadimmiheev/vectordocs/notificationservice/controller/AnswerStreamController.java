package com.github.vadimmiheev.vectordocs.notificationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.Map;

@Controller
@Slf4j
public class AnswerStreamController {

    @MessageMapping("search.result")
    public Flux<Void> searchResultsHandler(Flux<String> tokens, @Header(name = "metadata", required = false) Map<String, Object> metadata)
    {
        log.info("User ID: {}", metadata.get("userId"));
        return tokens
                .doOnNext(System.out::println) //TODO replace with UI communication
                .doOnComplete(System.out::println)
                .then()
                .flux();
    }
}
