package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.AuthSession;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    Optional<AuthSession> findById(Long id);

    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    boolean revokeIfActive(Long id, Instant now);

    void revokeAllByUserId(Long userId, Instant now);

    void deleteExpiredBefore(Instant instant);
}
