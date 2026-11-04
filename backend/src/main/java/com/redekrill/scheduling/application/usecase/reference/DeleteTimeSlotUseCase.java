/*
 * Exclusão de uma faixa horária, com uma porta só.
 *
 * Não há lista de faixas obrigatórias — diferente dos tipos de paletização, nenhuma
 * faixa é exigida pelo domínio, e o sistema funciona com o cadastro vazio. O que não
 * pode existir é faixa em uso: agendamento guarda o timeSlotId, e apagar a faixa deixa
 * essa referência órfã.
 *
 * O label da faixa removida vai para a auditoria, lido da instância carregada antes do
 * delete — depois de sumir, "08:00 - 10:00" não é mais reconstruível por id.
 */
package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

/**
 * Remove uma faixa horária sem agendamentos vinculados.
 */
public class DeleteTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public DeleteTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                 AppointmentRepository appointmentRepository,
                                 AuditService auditService,
                                 TransactionRunner transactionRunner) {
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public void execute(Long id, String actorEmail) {
        TimeSlot existing = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa horária não encontrada."));
        if (appointmentRepository.existsByTimeSlotId(id)) {
            throw new ConflictException(
                    "A faixa horária possui agendamentos vinculados e não pode ser excluída.");
        }

        transactionRunner.execute(() -> {
            timeSlotRepository.deleteById(id);
            auditService.record(actorEmail, AuditAction.DELETE, "TimeSlot", id,
                    "Faixa horária excluída: " + existing.label());
            return null;
        });
    }
}
