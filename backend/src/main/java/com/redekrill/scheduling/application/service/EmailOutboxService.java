package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Grava a intenção de envio na mesma transação do agendamento. O envio efetivo
 * é realizado pelo processador com retry, sem bloquear o cadastro.
 */
public class EmailOutboxService {

    private final EmailOutboxRepository repository;
    private final Clock clock;

    public EmailOutboxService(EmailOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void enqueue(Appointment appointment, List<String> warnings) {
        String joinedWarnings = warnings == null ? "" : String.join("\n", warnings);
        repository.save(EmailOutboxMessage.create(
                appointment.id(),
                appointment.supplierEmail(),
                joinedWarnings,
                Instant.now(clock)
        ));
    }
}
