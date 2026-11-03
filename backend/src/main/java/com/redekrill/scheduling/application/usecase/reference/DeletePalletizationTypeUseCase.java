package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

public class DeletePalletizationTypeUseCase {

    private final PalletizationTypeRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public DeletePalletizationTypeUseCase(PalletizationTypeRepository repository,
                                          AppointmentRepository appointmentRepository,
                                          AuditService auditService,
                                          TransactionRunner transactionRunner) {
        this.repository = repository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public void execute(Long id, String actorEmail) {
        PalletizationType existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tipo de paletização não encontrado."));
        if (existing.mandatory()) {
            throw new ConflictException(
                    "Os tipos Paletizado e Não Paletizado são obrigatórios e não podem ser excluídos.");
        }
        if (appointmentRepository.existsByPalletizationTypeId(id)) {
            throw new ConflictException(
                    "O tipo de paletização possui agendamentos vinculados e não pode ser excluído.");
        }

        transactionRunner.execute(() -> {
            repository.deleteById(id);
            auditService.record(actorEmail, AuditAction.DELETE, "PalletizationType", id,
                    "Tipo de paletização excluído: " + existing.name());
            return null;
        });
    }
}
