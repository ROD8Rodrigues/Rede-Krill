/*
 * Cadastro de uma faixa horária, onde a sobreposição é a única briga.
 *
 * O candidate com id nulo é montado só para uma coisa: perguntar ao modelo se ele
 * invade alguma faixa existente. Isso mantém a definição de conflito (encostar não é
 * invadir) em TimeSlot.overlaps, e não duplicada aqui.
 *
 * A comparação lê todas as faixas e roda em memória. É uma tabela de poucas linhas:
 * o dia tem doze janelas de duas horas no máximo — e o ganho de uma consulta de
 * intervalo no banco não pagaria a perda de legibilidade agora.
 *
 * Horário invertido e duração diferente de duas horas são recusados pelo construtor de
 * TimeSlot, e por isso este método não tem uma única linha de validação de campos.
 */
package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.CreateTimeSlotCommand;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

/**
 * Cria uma faixa horária que não se choque com as existentes.
 */
public class CreateTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public CreateTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                 AuditService auditService,
                                 TransactionRunner transactionRunner) {
        this.timeSlotRepository = timeSlotRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public TimeSlotResult execute(CreateTimeSlotCommand command) {
        TimeSlot candidate = new TimeSlot(null, command.startTime(), command.endTime());
        boolean overlaps = timeSlotRepository.findAll().stream()
                .anyMatch(existing -> existing.overlaps(candidate));
        if (overlaps) {
            throw new ConflictException("A faixa horária se sobrepõe a outra já cadastrada.");
        }

        TimeSlot saved = transactionRunner.execute(() -> {
            TimeSlot created = timeSlotRepository.save(candidate);
            auditService.record(command.actorEmail(), AuditAction.CREATE, "TimeSlot", created.id(),
                    "Faixa horária criada: " + created.label());
            return created;
        });
        return TimeSlotResult.from(saved);
    }
}
