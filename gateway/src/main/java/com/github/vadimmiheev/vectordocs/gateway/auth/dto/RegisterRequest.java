package com.github.vadimmiheev.vectordocs.gateway.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    @Email
    @NotBlank
    private String email;

    public RegisterRequest() {}

}
