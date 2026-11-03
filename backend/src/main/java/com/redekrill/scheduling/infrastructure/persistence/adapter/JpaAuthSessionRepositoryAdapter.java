package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.AuthSessionPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.AuthSessionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaAuthSessionRepositoryAdapter implements AuthSessionRepository {

    private final AuthSessionJpaRepository repository;

    public JpaAuthSessionRepositoryAdapter(AuthSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        return AuthSessionPersistenceMapper.toDomain(
                repository.save(AuthSessionPersistenceMapper.toEntity(session)));
    }

    @Override
    public Optional<AuthSession> findById(Long id) {
        return repository.findById(id).map(AuthSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash) {
        return repository.findByRefreshTokenHash(refreshTokenHash)
                .map(AuthSessionPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId, Instant now) {
        repository.revokeAllByUserId(userId, now);
    }

    @Override
    @Transactional
    public boolean revokeIfActive(Long id, Instant now) {
        return repository.revokeIfActive(id, now) > 0;
    }

    @Override
    @Transactional
    public void deleteExpiredBefore(Instant instant) {
        repository.deleteExpiredBefore(instant);
    }
}
