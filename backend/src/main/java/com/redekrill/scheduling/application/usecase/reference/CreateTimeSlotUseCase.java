package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.CreateTimeSlotCommand;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

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
