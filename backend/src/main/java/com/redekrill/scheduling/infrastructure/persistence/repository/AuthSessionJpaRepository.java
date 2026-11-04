/*
 * Consultas de sessão, onde as três operações em lote são o ponto que merece
 * atenção.
 *
 * revokeIfActive e revokeAllByUserId são update coletivo que só toca linha com revoked_at
 * nulo, e por isso servem para duas coisas ao mesmo tempo: revogar e responder se
 * realmente havia algo a revogar. revokeIfActive é justamente o teste de corrida da
 * rotação de token — zero linhas afetadas significou que outro fio chegou primeiro.
 *
 * deleteExpiredBefore apaga por data de expiração, e é o único delete da aplicação em
 * tabela de sessão: linha revogada continua lá, porque é a trilha de quem saiu.
 *
 * @Modifying aparece nos três porque update e delete em lote não passam por estado
 * gerenciado, e o contexto transacional é do adaptador.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.AuthSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositório Spring Data da entidade de sessão de login.
 */
public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionJpaEntity, Long> {

    Optional<AuthSessionJpaEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Modifying
    @Query("""
            update AuthSessionJpaEntity session
               set session.revokedAt = :now
             where session.id = :id
               and session.revokedAt is null
            """)
    int revokeIfActive(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("""
            update AuthSessionJpaEntity session
               set session.revokedAt = :now
             where session.userId = :userId
               and session.revokedAt is null
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from AuthSessionJpaEntity session where session.expiresAt < :instant")
    int deleteExpiredBefore(@Param("instant") Instant instant);
}
