package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Sessão renovável. O token de refresh nunca é persistido em claro: apenas o
 * hash SHA-256 é armazenado. O access token referencia o id desta sessão, o que
 * permite revogação imediata no logout.
 */
public record AuthSession(
        Long id,
        Long userId,
        String refreshTokenHash,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        String ipAddress,
        String userAgent
) {

    public AuthSession {
        Objects.requireNonNull(userId, "Usuário da sessão é obrigatório.");
        refreshTokenHash = DomainValidation.requireText(
                refreshTokenHash, "Hash do token de refresh é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação da sessão é obrigatória.");
        Objects.requireNonNull(expiresAt, "Data de expiração da sessão é obrigatória.");
        if (!expiresAt.isAfter(createdAt)) {
            throw new com.redekrill.scheduling.domain.exception.DomainException(
                    "Expiração da sessão deve ser posterior à criação.");
        }
        ipAddress = DomainValidation.optionalText(ipAddress, 64,
                "Endereço IP da sessão deve ter no máximo 64 caracteres.");
        userAgent = DomainValidation.optionalText(userAgent, 255,
                "User-Agent da sessão deve ter no máximo 255 caracteres.");
    }

    public static AuthSession create(Long userId, String refreshTokenHash, Instant now,
                                     Instant expiresAt, String ipAddress, String userAgent) {
        return new AuthSession(null, userId, refreshTokenHash, now, expiresAt, null,
                ipAddress, userAgent);
    }

    public boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public AuthSession revoke(Instant now) {
        return new AuthSession(id, userId, refreshTokenHash, createdAt, expiresAt, now,
                ipAddress, userAgent);
    }

    public AuthSession withId(Long newId) {
        return new AuthSession(newId, userId, refreshTokenHash, createdAt, expiresAt, revokedAt,
                ipAddress, userAgent);
    }
}
