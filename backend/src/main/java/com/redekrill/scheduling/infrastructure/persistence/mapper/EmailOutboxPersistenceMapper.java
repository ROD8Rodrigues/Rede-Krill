package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.infrastructure.persistence.entity.EmailOutboxJpaEntity;

public final class EmailOutboxPersistenceMapper {

    private EmailOutboxPersistenceMapper() {
    }

    public static EmailOutboxMessage toDomain(EmailOutboxJpaEntity entity) {
        return new EmailOutboxMessage(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getRecipient(),
                entity.getWarnings(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getSentAt()
        );
    }

    public static EmailOutboxJpaEntity toEntity(EmailOutboxMessage message) {
        EmailOutboxJpaEntity entity = new EmailOutboxJpaEntity();
        entity.setId(message.id());
        entity.setAppointmentId(message.appointmentId());
        entity.setRecipient(message.recipient());
        entity.setWarnings(message.warnings());
        entity.setStatus(message.status());
        entity.setAttempts(message.attempts());
        entity.setNextAttemptAt(message.nextAttemptAt());
        entity.setLastError(message.lastError());
        entity.setCreatedAt(message.createdAt());
        entity.setSentAt(message.sentAt());
        return entity;
    }
}
