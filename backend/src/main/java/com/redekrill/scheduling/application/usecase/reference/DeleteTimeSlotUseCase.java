package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

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
