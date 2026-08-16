package com.ofood.auth.repository;

import com.ofood.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

import com.ofood.auth.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.user = :user AND r.revokedAt IS NULL")
    void revokeAllByUser(@org.springframework.data.repository.query.Param("user") User user, @org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
