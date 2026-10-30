package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.dto.trucktype.UpdateTruckTypeCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

import java.time.Clock;
import java.time.Instant;

public class UpdateTruckTypeUseCase {

    private final TruckTypeRepository truckTypeRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public UpdateTruckTypeUseCase(TruckTypeRepository truckTypeRepository, AuditService auditService,
                                  TransactionRunner transactionRunner, Clock clock) {
        this.truckTypeRepository = truckTypeRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public TruckTypeResult execute(UpdateTruckTypeCommand command) {
        TruckType existing = truckTypeRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de caminhão não encontrado."));

        if (truckTypeRepository.existsByNameAndIdNot(command.name(), existing.id())) {
            throw new ConflictException("Já existe um tipo de caminhão com este nome.");
        }

        TruckType updated = existing.update(command.name(), command.description(), Instant.now(clock));

        TruckType saved = transactionRunner.execute(() -> {
            TruckType result = truckTypeRepository.save(updated);
            auditService.record(
                    command.actorEmail(),
                    AuditAction.UPDATE,
                    "TruckType",
                    result.id(),
                    "Tipo de caminhão atualizado: " + result.name()
            );
            return result;
        });

        return TruckTypeResult.from(saved);
    }
}
