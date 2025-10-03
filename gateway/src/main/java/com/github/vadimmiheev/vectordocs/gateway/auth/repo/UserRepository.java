package com.github.vadimmiheev.vectordocs.gateway.auth.repo;

import com.github.vadimmiheev.vectordocs.gateway.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
