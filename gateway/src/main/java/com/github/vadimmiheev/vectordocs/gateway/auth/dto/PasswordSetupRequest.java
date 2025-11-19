package com.github.vadimmiheev.vectordocs.gateway.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordSetupRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;
}