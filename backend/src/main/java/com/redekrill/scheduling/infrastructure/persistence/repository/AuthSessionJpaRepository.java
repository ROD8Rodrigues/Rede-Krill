package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.AuthSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

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
