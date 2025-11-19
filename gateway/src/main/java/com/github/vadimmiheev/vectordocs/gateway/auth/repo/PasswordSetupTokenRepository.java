package com.github.vadimmiheev.vectordocs.gateway.auth.repo;

import com.github.vadimmiheev.vectordocs.gateway.auth.model.PasswordSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {
    Optional<PasswordSetupToken> findByToken(String token);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndTokenNot(Long user_id, String token);
}
