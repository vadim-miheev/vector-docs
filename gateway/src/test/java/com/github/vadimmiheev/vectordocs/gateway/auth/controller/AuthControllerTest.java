package com.github.vadimmiheev.vectordocs.gateway.auth.controller;

import com.github.vadimmiheev.vectordocs.gateway.auth.dto.*;
import com.github.vadimmiheev.vectordocs.gateway.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        MessageResponse serviceResponse = new MessageResponse("Confirmation email sent to test@example.com.");
        when(authService.register(any(RegisterRequest.class))).thenReturn(serviceResponse);

        // Act & Assert
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Confirmation email sent to test@example.com.");
    }

    @Test
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email"); // Invalid email format

        // Act & Assert
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail(""); // Empty email

        // Act & Assert
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void passwordSetup_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("valid-token");
        request.setPassword("newPassword123");

        MessageResponse serviceResponse = new MessageResponse("Password set successfully. You can now log in.");
        when(authService.completePasswordSetup(any(PasswordSetupRequest.class))).thenReturn(serviceResponse);

        // Act & Assert
        webTestClient.post()
                .uri("/auth/password-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Password set successfully. You can now log in.");
    }

    @Test
    void passwordSetup_EmptyToken_ReturnsBadRequest() throws Exception {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken(""); // Empty token
        request.setPassword("newPassword123");

        // Act & Assert
        webTestClient.post()
                .uri("/auth/password-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void passwordSetup_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("valid-token");
        request.setPassword(""); // Empty password

        // Act & Assert
        webTestClient.post()
                .uri("/auth/password-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void passwordResetRequest_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        MessageResponse serviceResponse = new MessageResponse("If an account with this email exists, a reset link has been sent.");
        when(authService.requestPasswordReset(any(String.class))).thenReturn(serviceResponse);

        // Act & Assert
        webTestClient.post()
                .uri("/auth/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("If an account with this email exists, a reset link has been sent.");
    }

    @Test
    void passwordResetRequest_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");

        // Act & Assert
        webTestClient.post()
                .uri("/auth/password-reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_ValidCredentials_ReturnsOk() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse serviceResponse = new AuthResponse(1L, "test@example.com", "jwt-token");
        when(authService.login(any(LoginRequest.class))).thenReturn(serviceResponse);

        // Act & Assert
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.token").isEqualTo("jwt-token");
    }

    @Test
    void login_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        // Act & Assert
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        // Act & Assert
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("");

        // Act & Assert
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        // Act & Assert
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Invalid email or password");
    }

    @Test
    void register_ServiceThrowsException_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("User with this email already exists"));

        // Act & Assert
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("User with this email already exists");
    }

    @Test
    void endpoints_InvalidHttpMethod_ReturnsMethodNotAllowed() throws Exception {
        // Test wrong HTTP method for each endpoint
        webTestClient.get()
                .uri("/auth/register")
                .exchange()
                .expectStatus().isEqualTo(405); // Method Not Allowed

        webTestClient.put()
                .uri("/auth/login")
                .exchange()
                .expectStatus().isEqualTo(405); // Method Not Allowed

        webTestClient.delete()
                .uri("/auth/password-setup")
                .exchange()
                .expectStatus().isEqualTo(405); // Method Not Allowed
    }

    @Test
    void endpoints_InvalidContentType_ReturnsUnsupportedMediaType() throws Exception {
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("plain text")
                .exchange()
                .expectStatus().isEqualTo(415); // Unsupported Media Type
    }

    @Test
    void endpoints_MissingBody_ReturnsBadRequest() throws Exception {
        // Test with missing request body
        webTestClient.post()
                .uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }
}