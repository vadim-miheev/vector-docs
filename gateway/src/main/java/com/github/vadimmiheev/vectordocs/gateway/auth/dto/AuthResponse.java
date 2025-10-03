package com.github.vadimmiheev.vectordocs.gateway.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuthResponse {
    private Long id;
    private String email;
    private String token;
}
