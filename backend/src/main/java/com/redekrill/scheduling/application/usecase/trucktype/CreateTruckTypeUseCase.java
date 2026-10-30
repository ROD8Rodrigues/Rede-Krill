package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.CreateTruckTypeCommand;
import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

import java.time.Clock;
import java.time.Instant;

public class CreateTruckTypeUseCase {

    private final TruckTypeRepository truckTypeRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreateTruckTypeUseCase(TruckTypeRepository truckTypeRepository, AuditService auditService,
                                  TransactionRunner transactionRunner, Clock clock) {
        this.truckTypeRepository = truckTypeRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public TruckTypeResult execute(CreateTruckTypeCommand command) {
        if (truckTypeRepository.existsByName(command.name())) {
            throw new ConflictException("Já existe um tipo de caminhão com este nome.");
        }

        TruckType truckType = TruckType.create(command.name(), command.description(), Instant.now(clock));

        TruckType saved = transactionRunner.execute(() -> {
            TruckType created = truckTypeRepository.save(truckType);
            auditService.record(
                    command.actorEmail(),
                    AuditAction.CREATE,
                    "TruckType",
                    created.id(),
                    "Tipo de caminhão criado: " + created.name()
            );
            return created;
        });

        return TruckTypeResult.from(saved);
    }
}
