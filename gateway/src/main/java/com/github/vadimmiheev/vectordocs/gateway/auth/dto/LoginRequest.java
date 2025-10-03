package com.github.vadimmiheev.vectordocs.gateway.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class LoginRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    public LoginRequest() {}

}
