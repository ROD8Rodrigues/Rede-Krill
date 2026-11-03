package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailOutboxRepository {

    EmailOutboxMessage save(EmailOutboxMessage message);

    Optional<EmailOutboxMessage> findById(Long id);

    List<EmailOutboxMessage> findDue(Instant now, int limit);
}
