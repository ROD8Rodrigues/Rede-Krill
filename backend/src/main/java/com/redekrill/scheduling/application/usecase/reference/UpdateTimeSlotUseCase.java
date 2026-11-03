package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.dto.reference.UpdateTimeSlotCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

public class UpdateTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public UpdateTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                 AuditService auditService,
                                 TransactionRunner transactionRunner) {
        this.timeSlotRepository = timeSlotRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public TimeSlotResult execute(UpdateTimeSlotCommand command) {
        TimeSlot existing = timeSlotRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Faixa horária não encontrada."));
        TimeSlot candidate = new TimeSlot(existing.id(), command.startTime(), command.endTime());
        boolean overlaps = timeSlotRepository.findAll().stream()
                .filter(other -> !other.id().equals(existing.id()))
                .anyMatch(other -> other.overlaps(candidate));
        if (overlaps) {
            throw new ConflictException("A faixa horária se sobrepõe a outra já cadastrada.");
        }

        TimeSlot saved = transactionRunner.execute(() -> {
            TimeSlot updated = timeSlotRepository.save(candidate);
            auditService.record(command.actorEmail(), AuditAction.UPDATE, "TimeSlot", updated.id(),
                    "Faixa horária atualizada: " + updated.label());
            return updated;
        });
        return TimeSlotResult.from(saved);
    }
}
