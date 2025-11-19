package com.github.vadimmiheev.vectordocs.gateway.auth.service;

import com.github.vadimmiheev.vectordocs.gateway.auth.dto.AuthResponse;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.LoginRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.MessageResponse;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.PasswordSetupRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.dto.RegisterRequest;
import com.github.vadimmiheev.vectordocs.gateway.auth.model.PasswordSetupToken;
import com.github.vadimmiheev.vectordocs.gateway.auth.model.User;
import com.github.vadimmiheev.vectordocs.gateway.auth.repo.PasswordSetupTokenRepository;
import com.github.vadimmiheev.vectordocs.gateway.auth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordSetupTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService;
    private final MailService mailService;

    @Value("${app.ui.host.public}")
    private String uiHost;

    @Value("${app.password-setup.ttl-seconds:86400}")
    private long passwordSetupTtlSeconds;

    public AuthService(UserRepository userRepository,
                       PasswordSetupTokenRepository tokenRepository,
                       JwtService jwtService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.isEmailVerified()) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        if (user == null) {
            user = new User(email);
            user = userRepository.save(user);
        }

        String subject = "Confirm your email and set password";
        String textPrefix = "Hello,\n\nPlease set your password to activate your account: ";
        createPasswordSetupTokenAndSendMail(user, subject, textPrefix);

        return new MessageResponse("Confirmation email sent to " + email + ".");
    }

    @Transactional
    public MessageResponse completePasswordSetup(PasswordSetupRequest request) {
        PasswordSetupToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (token.isUsed() || token.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true);
        token.setUsedAt(Instant.now());

        userRepository.save(user);
        tokenRepository.save(token);
        // Remove other tokens
        tokenRepository.deleteByUserIdAndTokenNot(user.getId(), token.getToken());

        return new MessageResponse("Password set successfully. You can now log in.");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Email is not verified. Please check your inbox.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }

    /**
     * Initiates password reset flow by sending email with password setup link.
     * Does not reveal whether the email exists in the system.
     */
    @Transactional
    public MessageResponse requestPasswordReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            String subject = "Password reset";
            String textPrefix = """
                    Hello,
                    
                    We received a request to reset your password. \
                    You can set a new password using this link:\s""";
            createPasswordSetupTokenAndSendMail(user, subject, textPrefix);
        }

        // Always return success message to avoid user enumeration
        return new MessageResponse("If an account with this email exists, a reset link has been sent.");
    }

    private String buildPasswordSetupLink(String token) {
        return uiHost + "/password-setup?token=" + token;
    }

    private static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    private String normalizeEmail(String rawEmail) {
        return (rawEmail == null ? "" : rawEmail).trim().toLowerCase();
    }

    private void createPasswordSetupTokenAndSendMail(User user, String subject, String textPrefix) {
        // Invalidate previous tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        PasswordSetupToken token = new PasswordSetupToken();
        token.setToken(generateToken());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(passwordSetupTtlSeconds, ChronoUnit.SECONDS));
        tokenRepository.save(token);

        String link = buildPasswordSetupLink(token.getToken());
        String text = textPrefix + link +
                "\n\nThis link will expire in " + (passwordSetupTtlSeconds / 3600) + " hours.";

        mailService.send(user.getEmail(), subject, text);
    }
}