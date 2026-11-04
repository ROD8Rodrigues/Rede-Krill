/*
 * Cadastro de um tipo de paletização avulso.
 *
 * O código do registro criado é sempre OUTRO. Os dois tipos obrigatórios do domínio
 * — Paletizado e Não Paletizado — nascem do script de referência e têm multiplicador
 * preso ao código; não existe rota para criá-los por aqui, e isso é o que torna
 * possível o resto do método.
 *
 * O multiplicador nulo vira 1 antes de chegar ao construtor do modelo, porque o
 * formulário pode não ter o campo preenchido. A faixa de 1 a 100 continua sendo
 * cobrada por PalletizationType, que é quem manda no valor.
 *
 * O nome é aparado e comparado sem diferenciar maiúsculas; a checagem corre fora da
 * transação, porque não escrever nada também é resposta.
 */
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

/**
 * Cria um tipo de paletização com código livre.
 */
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
