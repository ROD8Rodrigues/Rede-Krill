/*
 * Mensagem da fila de confirmações entre a tabela e o modelo.
 *
 * Dez campos, e o que importa entender é o appointmentId: coluna simples, sem
 * @ManyToOne. A fila não é parte do agendamento — é o registro do que aconteceu com
 * o aviso — e amarrá-la por relação faria a varredura do processador carregar o
 * agendamento inteiro para cada mensagem.
 *
 * attempts, nextAttemptAt e lastError atravessam como estão. São eles que dão ao
 * processador a capacidade de decidir se uma mensagem ainda é reenviável sem consultar
 * nada além da própria linha.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.infrastructure.persistence.entity.EmailOutboxJpaEntity;

/**
 * Conversão entre EmailOutboxJpaEntity e o modelo EmailOutboxMessage.
 */
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
