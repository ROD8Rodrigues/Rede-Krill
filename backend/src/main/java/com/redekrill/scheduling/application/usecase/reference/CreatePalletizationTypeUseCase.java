package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.CreatePalletizationTypeCommand;
import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

public class CreatePalletizationTypeUseCase {

    private final PalletizationTypeRepository repository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public CreatePalletizationTypeUseCase(PalletizationTypeRepository repository,
                                          AuditService auditService,
                                          TransactionRunner transactionRunner) {
        this.repository = repository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public PalletizationTypeResult execute(CreatePalletizationTypeCommand command) {
        String name = command.name() == null ? null : command.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Já existe um tipo de paletização com este nome.");
        }
        int multiplier = command.capacityMultiplier() == null
                ? 1 : command.capacityMultiplier();

        PalletizationType candidate = new PalletizationType(
                null, PalletizationCode.OUTRO, name, multiplier);

        PalletizationType saved = transactionRunner.execute(() -> {
            PalletizationType created = repository.save(candidate);
            auditService.record(command.actorEmail(), AuditAction.CREATE,
                    "PalletizationType", created.id(),
                    "Tipo de paletização criado: " + created.name());
            return created;
        });
        return PalletizationTypeResult.from(saved);
    }
}
