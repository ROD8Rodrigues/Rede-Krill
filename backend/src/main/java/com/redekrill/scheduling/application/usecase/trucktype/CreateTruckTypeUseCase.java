/*
 * Cadastro de um tipo de caminhão — o mais simples dos quatro.
 *
 * Nome e descrição, e nenhuma regra de capacidade envolvida: o veículo não altera a conta de
 * pallets, que depende da quantidade declarada e do multiplicador da paletização. Por isso não
 * há validação de peso, eixos ou cubagem — nada disso é requisito do teste, e inventar regra
 * aqui seria justamente o tipo de extrapolação a evitar.
 *
 * A descrição é opcional: vazia ou só espaço vira null no modelo, em vez de string
 * vazia poluindo a tabela.
 */
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

/**
 * Cadastra um tipo de caminhão com nome único.
 */
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
