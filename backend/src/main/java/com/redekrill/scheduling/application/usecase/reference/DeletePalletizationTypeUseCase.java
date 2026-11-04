/*
 * Exclusão de um tipo avulso, com duas portas antes da faca.
 *
 * A primeira protege a regra de negócio: Paletizado e Não Paletizado são obrigatórios
 * e não saem do catálogo, porque o cálculo de capacidade e o seed dependem deles. A
 * segunda protege a integridade: se existe agendamento apontando para o tipo, ele
 * continua lá mesmo depois de apagado, e aí a leitura quebra.
 *
 * O delete é físico, e por isso a auditoria é gravada depois dele, dentro da mesma
 * transação — o nome do tipo removido é lido da instância carregada antes de sumir. A
 * linha de log não tem FOREIGN KEY com o cadastro, então sobrevive ao registro.
 */
package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

/**
 * Remove um tipo de paletização não obrigatório e sem uso.
 */
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
