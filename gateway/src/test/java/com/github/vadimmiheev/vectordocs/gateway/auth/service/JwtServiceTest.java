package com.github.vadimmiheev.vectordocs.gateway.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-secret-key-1234567890";
    private final long ttlSeconds = 3600L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, ttlSeconds);
    }

    @Test
    void generateToken_ValidInput_ReturnsValidToken() {
        // Arrange
        Long userId = 123L;
        String email = "test@example.com";

        // Act
        String token = jwtService.generateToken(userId, email);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Verify token can be decoded
        DecodedJWT decodedJWT = JWT.decode(token);
        assertEquals(userId, decodedJWT.getClaim("uid").asLong());
        assertEquals(email, decodedJWT.getClaim("email").asString());
        assertNotNull(decodedJWT.getIssuedAt());
        assertNotNull(decodedJWT.getExpiresAt());

        // Check expiration is in the future
        assertTrue(decodedJWT.getExpiresAt().after(new Date()));
    }

    @Test
    void generateToken_DifferentUsers_DifferentTokens() {
        // Arrange
        Long userId1 = 123L;
        String email1 = "user1@example.com";
        Long userId2 = 456L;
        String email2 = "user2@example.com";

        // Act
        String token1 = jwtService.generateToken(userId1, email1);
        String token2 = jwtService.generateToken(userId2, email2);

        // Assert
        assertNotEquals(token1, token2);

        DecodedJWT decoded1 = JWT.decode(token1);
        DecodedJWT decoded2 = JWT.decode(token2);

        assertEquals(userId1, decoded1.getClaim("uid").asLong());
        assertEquals(email1, decoded1.getClaim("email").asString());
        assertEquals(userId2, decoded2.getClaim("uid").asLong());
        assertEquals(email2, decoded2.getClaim("email").asString());
    }

    @Test
    void verifyToken_ValidToken_ReturnsDecodedJWT() {
        // Arrange
        Long userId = 123L;
        String email = "test@example.com";
        String token = jwtService.generateToken(userId, email);

        // Act
        DecodedJWT decodedJWT = jwtService.verifyToken(token);

        // Assert
        assertNotNull(decodedJWT);
        assertEquals(userId, decodedJWT.getClaim("uid").asLong());
        assertEquals(email, decodedJWT.getClaim("email").asString());
        assertNotNull(decodedJWT.getIssuedAt());
        assertNotNull(decodedJWT.getExpiresAt());
    }

    @Test
    void verifyToken_TokenFromDifferentSecret_ThrowsException() {
        // Arrange
        // Create token with different secret
        Algorithm differentAlgorithm = Algorithm.HMAC256("different-secret");
        String token = JWT.create()
                .withClaim("uid", 123L)
                .withClaim("email", "test@example.com")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(differentAlgorithm);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.verifyToken(token);
        });
        // The exact exception type depends on JWT library implementation
        assertNotNull(exception);
    }

    @Test
    void verifyToken_ExpiredToken_ThrowsException() {
        // Arrange
        // Create expired token manually
        Algorithm algorithm = Algorithm.HMAC256(secret);
        String token = JWT.create()
                .withClaim("uid", 123L)
                .withClaim("email", "test@example.com")
                .withIssuedAt(Date.from(Instant.now().minusSeconds(7200))) // 2 hours ago
                .withExpiresAt(Date.from(Instant.now().minusSeconds(3600))) // 1 hour ago
                .sign(algorithm);

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.verifyToken(token);
        });
        assertNotNull(exception);
    }

    @Test
    void verifyToken_MalformedToken_ThrowsException() {
        // Arrange
        String malformedToken = "malformed.token.string";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.verifyToken(malformedToken);
        });
        assertNotNull(exception);
    }

    @Test
    void verifyToken_EmptyToken_ThrowsException() {
        // Arrange
        String emptyToken = "";

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.verifyToken(emptyToken);
        });
        assertNotNull(exception);
    }

    @Test
    void verifyToken_NullToken_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            jwtService.verifyToken(null);
        });
        assertNotNull(exception);
    }

    @Test
    void tokenContainsCorrectClaims() {
        // Arrange
        Long userId = 999L;
        String email = "special@example.com";

        // Act
        String token = jwtService.generateToken(userId, email);
        DecodedJWT decodedJWT = JWT.decode(token);

        // Assert
        assertEquals(userId, decodedJWT.getClaim("uid").asLong());
        assertEquals(email, decodedJWT.getClaim("email").asString());
        // Non-existent claim should be null/missing
        // Note: getClaim("nonexistent") returns a Claim object, not null
        // We can check that it's null when converted to appropriate type
        assertNull(decodedJWT.getClaim("nonexistent").asString());
        assertNull(decodedJWT.getClaim("nonexistent").asLong());
    }

    @Test
    void tokenExpirationTimeIsCorrect() {
        // Arrange
        Long userId = 123L;
        String email = "test@example.com";
        Instant beforeGeneration = Instant.now();

        // Act
        String token = jwtService.generateToken(userId, email);
        DecodedJWT decodedJWT = JWT.decode(token);
        Instant issuedAt = decodedJWT.getIssuedAt().toInstant();
        Instant expiresAt = decodedJWT.getExpiresAt().toInstant();

        // Assert
        // Issued at should be close to now (within 1 second)
        assertTrue(issuedAt.isAfter(beforeGeneration.minusSeconds(1)));
        assertTrue(issuedAt.isBefore(Instant.now().plusSeconds(1)));

        // Expiration should be ttlSeconds after issued at
        long actualTtl = expiresAt.getEpochSecond() - issuedAt.getEpochSecond();
        assertEquals(ttlSeconds, actualTtl);
    }

    @Test
    void constructor_WithDefaultValues_Works() {
        // Test that constructor works with default values (simulating @Value with defaults)
        JwtService serviceWithDefaults = new JwtService("default-secret", 86400L);
        assertNotNull(serviceWithDefaults);

        String token = serviceWithDefaults.generateToken(1L, "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}