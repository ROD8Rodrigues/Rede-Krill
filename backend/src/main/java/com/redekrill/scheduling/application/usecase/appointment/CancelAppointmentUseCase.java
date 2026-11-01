package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
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
 */
public class CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CancelAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    UserRepository userRepository,
                                    AuditService auditService,
                                    TransactionRunner transactionRunner,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
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
