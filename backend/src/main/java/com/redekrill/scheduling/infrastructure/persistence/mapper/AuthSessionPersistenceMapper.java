/*
 * Sessão de autenticação entre a tabela e o modelo.
 *
 * Os oito campos passam idênticos, com uma ausência que merece leitura: não há
 * relação com User aqui. A entidade guarda userId como coluna simples, e o modelo
 * AuthSession faz o mesmo. A razão é prática: a sessão é lida e revogada pelo número do
 * usuário, em update coletivo, e quem precisa dos dados da pessoa resolve por
 * UserRepository. Sem a relação mapeada, a tabela de sessão não depende de carregar nada.
 *
 * O hash do refresh token atravessa como texto de 64 caracteres. Nem este mapper nem
 * o modelo conhecem o algoritmo: quem gera e quem compara está no port
 * RefreshTokenGenerator.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.infrastructure.persistence.entity.AuthSessionJpaEntity;

/**
 * Conversão entre AuthSessionJpaEntity e o modelo AuthSession.
 */
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
