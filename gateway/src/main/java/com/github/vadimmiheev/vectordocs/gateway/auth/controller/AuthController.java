package com.github.vadimmiheev.vectordocs.gateway.auth.controller;

import com.github.vadimmiheev.vectordocs.gateway.auth.dto.AuthResponse;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.LoginRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.RegisterRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Mono.fromCallable(() -> authService.register(request))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> authService.login(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
