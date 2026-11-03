package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.UpdatePalletizationTypeCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

public class UpdatePalletizationTypeUseCase {

    private final PalletizationTypeRepository repository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public UpdatePalletizationTypeUseCase(PalletizationTypeRepository repository,
                                          AuditService auditService,
                                          TransactionRunner transactionRunner) {
        this.repository = repository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public PalletizationTypeResult execute(UpdatePalletizationTypeCommand command) {
        PalletizationType existing = repository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de paletização não encontrado."));
        String name = command.name() == null ? null : command.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, existing.id())) {
            throw new ConflictException("Já existe um tipo de paletização com este nome.");
        }

        int multiplier = existing.capacityMultiplier();
        if (!existing.mandatory() && command.capacityMultiplier() != null) {
            multiplier = command.capacityMultiplier();
        }
        PalletizationType candidate = new PalletizationType(
                existing.id(), existing.code(), name, multiplier);

        PalletizationType saved = transactionRunner.execute(() -> {
            PalletizationType updated = repository.save(candidate);
            auditService.record(command.actorEmail(), AuditAction.UPDATE,
                    "PalletizationType", updated.id(),
                    "Tipo de paletização atualizado: " + updated.name());
            return updated;
        });
        return PalletizationTypeResult.from(saved);
    }
}
