package com.github.vadimmiheev.vectordocs.gateway.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

    public RegisterRequest() {}

}
