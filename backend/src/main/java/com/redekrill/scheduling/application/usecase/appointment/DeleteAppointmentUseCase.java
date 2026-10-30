package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

public class DeleteAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;

    public DeleteAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    AuditService auditService,
                                    TransactionRunner transactionRunner) {
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
    }

    public void execute(Long id, String actorEmail) {
        if (id == null) {
            throw new DomainException("Identificador do agendamento é obrigatório.");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        transactionRunner.execute(() -> {
            appointmentRepository.deleteById(id);
            auditService.record(
                    actorEmail,
                    AuditAction.DELETE,
                    "Appointment",
                    id,
                    "Agendamento excluído: fornecedor=%s, data=%s, faixa=%s"
                            .formatted(appointment.supplierName(), appointment.date(),
                                    appointment.timeSlot().label())
            );
            return null;
        });
    }
}
