/*
 * O cancelamento, que é lógico e não um delete.
 *
 * A linha continua na tabela com status CANCELED, e é isso que permite ao relatório
 * mostrar quantos agendamentos foram desfeitos no período e às somas de capacidade
 * ignorá-los — as consultas de total filtram por status ativo.
 *
 * O detalhe que separa este caso de uso de um simples update de status é a fila:
 * cancelPending derruba as confirmações ainda não enviadas daquele agendamento. Sem
 * isso, o processador acordaria e avisaria o fornecedor sobre uma hora que já não
 * vale. Mensagens já enviadas ficam como estão; o que passou, passou.
 *
 * Tudo acontece dentro de uma transação, incluindo a auditoria, porque cancelamento
 * sem trilha é exatamente o tipo de coisa que alguém vai negar depois.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.EmailOutboxService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Cancelamento lógico. O registro é preservado para histórico e relatórios.
 * As confirmações de e-mail ainda pendentes são suprimidas na mesma transação.
 */
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailOutboxService emailOutboxService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CancelAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    UserRepository userRepository,
                                    AuditService auditService,
                                    EmailOutboxService emailOutboxService,
                                    TransactionRunner transactionRunner,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailOutboxService = emailOutboxService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public AppointmentResult execute(Long id, String actorEmail) {
        if (id == null) {
            throw new DomainException("Identificador do agendamento é obrigatório.");
        }
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }

        return transactionRunner.execute(() -> {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
            User actor = userRepository.findByEmail(actorEmail)
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário autenticado não encontrado ou inativo."));

            Appointment canceled = appointment.cancel(actor, Instant.now(clock));
            Appointment saved = appointmentRepository.save(canceled);
            emailOutboxService.cancelPending(saved.id());

            auditService.record(
                    actor.email(),
                    AuditAction.CANCEL,
                    "Appointment",
                    saved.id(),
                    "Agendamento cancelado: ID Pedido=%d, fornecedor=%s, data=%s, faixa=%s"
                            .formatted(saved.orderId(), saved.supplierName(), saved.date(),
                                    saved.timeSlot().label())
            );

            return AppointmentResult.from(saved);
        });
    }
}
