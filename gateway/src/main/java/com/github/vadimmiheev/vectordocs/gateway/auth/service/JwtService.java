package com.github.vadimmiheev.vectordocs.gateway.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final Algorithm algorithm;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret:secret}") String secret,
            @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return JWT.create()
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .withClaim("uid", userId)
                .withClaim("email", email)
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}
