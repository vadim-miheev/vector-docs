package com.github.vadimmiheev.vectordocs.gateway.auth.service;

import com.github.vadimmiheev.vectordocs.gateway.auth.dto.AuthResponse;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.LoginRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.RegisterRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.model.User;
import com.github.vadimmiheev.vectordocs.gateway.auth.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        String hash = passwordEncoder.encode(request.getPassword());
        User user = new User(email, hash);
        user = userRepository.save(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }
}
