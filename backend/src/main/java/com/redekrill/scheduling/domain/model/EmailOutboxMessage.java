package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EmailOutboxMessage(
        Long id,
        Long appointmentId,
        String recipient,
        String warnings,
        EmailOutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant sentAt
) {

    public EmailOutboxMessage {
        Objects.requireNonNull(appointmentId, "Agendamento da mensagem é obrigatório.");
        recipient = DomainValidation.requireEmail(recipient, "Destinatário é obrigatório.");
        warnings = warnings == null ? "" : warnings;
        Objects.requireNonNull(status, "Status da mensagem é obrigatório.");
        Objects.requireNonNull(nextAttemptAt, "Próxima tentativa é obrigatória.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        if (attempts < 0) {
            throw new com.redekrill.scheduling.domain.exception.DomainException(
                    "Tentativas não podem ser negativas.");
        }
    }

    public static EmailOutboxMessage create(Long appointmentId, String recipient,
                                            String warnings, Instant now) {
        return new EmailOutboxMessage(null, appointmentId, recipient, warnings,
                EmailOutboxStatus.PENDING, 0, now, null, now, null);
    }

    public EmailOutboxMessage withId(Long newId) {
        return new EmailOutboxMessage(newId, appointmentId, recipient, warnings, status,
                attempts, nextAttemptAt, lastError, createdAt, sentAt);
    }

    public EmailOutboxMessage markSent(Instant now) {
        return new EmailOutboxMessage(id, appointmentId, recipient, warnings,
                EmailOutboxStatus.SENT, attempts + 1, now, null, createdAt, now);
    }

    public EmailOutboxMessage markFailed(Instant now, String error, int maxAttempts,
                                         Instant nextAttemptAt) {
        EmailOutboxStatus newStatus = attempts + 1 >= maxAttempts
                ? EmailOutboxStatus.FAILED : EmailOutboxStatus.PENDING;
        String truncatedError = error == null ? null
                : error.substring(0, Math.min(error.length(), 1000));
        return new EmailOutboxMessage(id, appointmentId, recipient, warnings,
                newStatus, attempts + 1, nextAttemptAt, truncatedError, createdAt, null);
    }
}
