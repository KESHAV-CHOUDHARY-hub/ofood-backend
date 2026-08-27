package com.ofood.auth.repository;

import com.ofood.auth.model.PasswordResetToken;
import com.ofood.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.invalidatedAt = :now WHERE t.user = :user AND t.usedAt IS NULL AND t.invalidatedAt IS NULL")
    void invalidateAllActiveTokensForUser(@Param("user") User user, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.invalidatedAt IS NULL AND t.expiresAt > :now")
    int consumeTokenAtomic(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
