package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.EmailOutboxPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.EmailOutboxJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaEmailOutboxRepositoryAdapter implements EmailOutboxRepository {

    private final EmailOutboxJpaRepository repository;

    public JpaEmailOutboxRepositoryAdapter(EmailOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmailOutboxMessage save(EmailOutboxMessage message) {
        return EmailOutboxPersistenceMapper.toDomain(
                repository.save(EmailOutboxPersistenceMapper.toEntity(message)));
    }

    @Override
    public Optional<EmailOutboxMessage> findById(Long id) {
        return repository.findById(id).map(EmailOutboxPersistenceMapper::toDomain);
    }

    @Override
    public List<EmailOutboxMessage> findDue(Instant now, int limit) {
        return repository.findDue(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(EmailOutboxPersistenceMapper::toDomain)
                .toList();
    }
}
