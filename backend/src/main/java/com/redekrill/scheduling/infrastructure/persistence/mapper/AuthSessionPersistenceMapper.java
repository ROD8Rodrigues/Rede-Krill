package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.infrastructure.persistence.entity.AuthSessionJpaEntity;

public final class AuthSessionPersistenceMapper {

    private AuthSessionPersistenceMapper() {
    }

    public static AuthSession toDomain(AuthSessionJpaEntity entity) {
        return new AuthSession(
                entity.getId(),
                entity.getUserId(),
                entity.getRefreshTokenHash(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getIpAddress(),
                entity.getUserAgent()
        );
    }

    public static AuthSessionJpaEntity toEntity(AuthSession session) {
        AuthSessionJpaEntity entity = new AuthSessionJpaEntity();
        entity.setId(session.id());
        entity.setUserId(session.userId());
        entity.setRefreshTokenHash(session.refreshTokenHash());
        entity.setCreatedAt(session.createdAt());
        entity.setExpiresAt(session.expiresAt());
        entity.setRevokedAt(session.revokedAt());
        entity.setIpAddress(session.ipAddress());
        entity.setUserAgent(session.userAgent());
        return entity;
    }
}
