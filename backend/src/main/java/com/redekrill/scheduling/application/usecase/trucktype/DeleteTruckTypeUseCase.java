/*
 * Exclusão de um tipo de caminhão, barrada por uso.
 *
 * A checagem existsByTruckTypeId é a diferença entre um delete seguro e uma
 * referência pendurada: o agendamento guarda a FK do tipo, e um cadastro que some com
 * linhas apontando para ele quebra a leitura de quem consulta.
 *
 * O nome vai para a auditoria lido antes da remoção, e o transactionRunner envolve os
 * dois — apagar sem registrar seria perder a explicação de quem tirou o tipo do
 * catálogo.
 */
package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

/**
 * Remove um tipo de caminhão que nenhum agendamento usa.
 */
public class DeleteTruckTypeUseCase {

    private final TruckTypeRepository truckTypeRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public DeleteTruckTypeUseCase(TruckTypeRepository truckTypeRepository,
                                  AppointmentRepository appointmentRepository,
                                  AuditService auditService, TransactionRunner transactionRunner) {
        this.truckTypeRepository = truckTypeRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public void execute(Long id, String actorEmail) {
        TruckType truckType = truckTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de caminhão não encontrado."));

        if (appointmentRepository.existsByTruckTypeId(id)) {
            throw new ConflictException("Tipo de caminhão em uso por um agendamento e não pode ser excluído.");
        }

        transactionRunner.execute(() -> {
            truckTypeRepository.deleteById(id);
            auditService.record(
                    actorEmail,
                    AuditAction.DELETE,
                    "TruckType",
                    id,
                    "Tipo de caminhão excluído: " + truckType.name()
            );
            return null;
        });
    }
}
