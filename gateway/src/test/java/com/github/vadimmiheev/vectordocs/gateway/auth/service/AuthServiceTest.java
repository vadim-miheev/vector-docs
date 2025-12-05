package com.github.vadimmiheev.vectordocs.gateway.auth.service;

import com.github.vadimmiheev.vectordocs.gateway.auth.dto.*;
import com.github.vadimmiheev.vectordocs.gateway.auth.model.PasswordSetupToken;
import com.github.vadimmiheev.vectordocs.gateway.auth.model.User;
import com.github.vadimmiheev.vectordocs.gateway.auth.repo.PasswordSetupTokenRepository;
import com.github.vadimmiheev.vectordocs.gateway.auth.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordSetupTokenRepository tokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private MailService mailService;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, tokenRepository, jwtService, mailService);
    }

    @Test
    void register_NewUser_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("Confirmation email sent to test@example.com.", response.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).deleteByUserId(any());
        verify(tokenRepository).save(any(PasswordSetupToken.class));
        verify(mailService).send(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void register_ExistingUnverifiedUser_ResendsEmail() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        User existingUser = new User("test@example.com");
        existingUser.setEmailVerified(false);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("Confirmation email sent to test@example.com.", response.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class)); // User already exists
        verify(tokenRepository).deleteByUserId(any());
        verify(tokenRepository).save(any(PasswordSetupToken.class));
        verify(mailService).send(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void register_ExistingVerifiedUser_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        User existingUser = new User("test@example.com");
        existingUser.setEmailVerified(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertEquals("User with this email already exists", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).deleteByUserId(any());
        verify(tokenRepository, never()).save(any(PasswordSetupToken.class));
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void completePasswordSetup_ValidToken_Success() {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("valid-token");
        request.setPassword("newPassword123");

        User user = new User("test@example.com");
        user.setId(1L);

        PasswordSetupToken token = new PasswordSetupToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponse response = authService.completePasswordSetup(request);

        // Assert
        assertNotNull(response);
        assertEquals("Password set successfully. You can now log in.", response.getMessage());
        assertNotNull(user.getPasswordHash());
        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());
        verify(tokenRepository).findByToken("valid-token");
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(tokenRepository).deleteByUserIdAndTokenNot(eq(1L), eq("valid-token"));
    }

    @Test
    void completePasswordSetup_InvalidToken_ThrowsException() {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("invalid-token");
        request.setPassword("newPassword123");

        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.completePasswordSetup(request));
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(tokenRepository).findByToken("invalid-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completePasswordSetup_ExpiredToken_ThrowsException() {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("expired-token");
        request.setPassword("newPassword123");

        User user = new User("test@example.com");

        PasswordSetupToken token = new PasswordSetupToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.completePasswordSetup(request));
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(tokenRepository).findByToken("expired-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completePasswordSetup_UsedToken_ThrowsException() {
        // Arrange
        PasswordSetupRequest request = new PasswordSetupRequest();
        request.setToken("used-token");
        request.setPassword("newPassword123");

        User user = new User("test@example.com");

        PasswordSetupToken token = new PasswordSetupToken();
        token.setToken("used-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setUsedAt(Instant.now()); // Already used

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.completePasswordSetup(request));
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(tokenRepository).findByToken("used-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ValidCredentials_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User("test@example.com");
        user.setId(1L);
        user.setEmailVerified(true);
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(1L, "test@example.com")).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        verify(userRepository).findByEmail("test@example.com");
        verify(jwtService).generateToken(1L, "test@example.com");
    }

    @Test
    void login_InvalidEmail_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(jwtService, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void login_UnverifiedEmail_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User("test@example.com");
        user.setEmailVerified(false);
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.login(request));
        assertEquals("Email is not verified. Please check your inbox.", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(jwtService, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        User user = new User("test@example.com");
        user.setEmailVerified(true);
        user.setPasswordHash(passwordEncoder.encode("password123")); // Different password

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(jwtService, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void requestPasswordReset_ExistingUser_SendsEmail() {
        // Arrange
        String email = "test@example.com";
        User user = new User("test@example.com");
        user.setId(1L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        MessageResponse response = authService.requestPasswordReset(email);

        // Assert
        assertNotNull(response);
        assertEquals("If an account with this email exists, a reset link has been sent.", response.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(tokenRepository).deleteByUserId(1L);
        verify(tokenRepository).save(any(PasswordSetupToken.class));
        verify(mailService).send(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_NonExistingUser_ReturnsSuccessMessage() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        MessageResponse response = authService.requestPasswordReset(email);

        // Assert
        assertNotNull(response);
        assertEquals("If an account with this email exists, a reset link has been sent.", response.getMessage());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(tokenRepository, never()).deleteByUserId(any());
        verify(tokenRepository, never()).save(any(PasswordSetupToken.class));
        verify(mailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordReset_EmptyEmail_ThrowsException() {
        // Arrange
        String email = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.requestPasswordReset(email));
        assertEquals("Email is required", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void normalizeEmail_ValidEmail_ReturnsNormalized() {
        // Test via the public register method
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  TEST@EXAMPLE.COM  ");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordSetupToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).findByEmail("test@example.com"); // Checking that email is normalized
    }

    @Test
    void normalizeEmail_NullEmail_ReturnsEmptyString() {
        // Test via requestPasswordReset
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.requestPasswordReset(null));
        assertEquals("Email is required", exception.getMessage());
    }
}