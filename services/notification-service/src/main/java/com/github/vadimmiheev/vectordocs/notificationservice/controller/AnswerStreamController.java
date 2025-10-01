package com.github.vadimmiheev.vectordocs.notificationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
public class AnswerStreamController {

    @MessageMapping("search.result")
    public Mono<Void> searchResultsHandler(Flux<String> tokens) {
        return tokens
                .doOnNext(System.out::println) //TODO replace with UI communication
                .doOnComplete(System.out::println)
                .then();
    }
}
