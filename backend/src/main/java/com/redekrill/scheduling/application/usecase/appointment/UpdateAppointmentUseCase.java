package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityResult;
import com.redekrill.scheduling.application.dto.appointment.UpdateAppointmentCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.policy.CapacityAssessment;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

import java.time.Clock;
import java.time.Instant;

public class UpdateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TruckTypeRepository truckTypeRepository;
    private final PalletizationTypeRepository palletizationTypeRepository;
    private final CapacityPolicy capacityPolicy;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public UpdateAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    TimeSlotRepository timeSlotRepository,
                                    TruckTypeRepository truckTypeRepository,
                                    PalletizationTypeRepository palletizationTypeRepository,
                                    CapacityPolicy capacityPolicy,
                                    AuditService auditService,
                                    TransactionRunner transactionRunner,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.truckTypeRepository = truckTypeRepository;
        this.palletizationTypeRepository = palletizationTypeRepository;
        this.capacityPolicy = capacityPolicy;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public AppointmentCommandResult execute(UpdateAppointmentCommand command) {
        validate(command);

        PreparedAppointment prepared = transactionRunner.execute(() -> {
            Appointment existing = appointmentRepository.findById(command.id())
                    .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

            TimeSlot timeSlot = timeSlotRepository.findById(command.timeSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Faixa horária não encontrada."));
            TruckType truckType = truckTypeRepository.findById(command.truckTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de caminhão não encontrado."));
            PalletizationType palletizationType = palletizationTypeRepository
                    .findById(command.palletizationTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de paletização não encontrado."));

            Appointment updated = existing.update(
                    command.supplierName(),
                    command.supplierEmail(),
                    command.truckPlate(),
                    command.date(),
                    timeSlot,
                    truckType,
                    palletizationType,
                    command.palletQuantity(),
                    Instant.now(clock)
            );

            long dailyPallets = appointmentRepository.sumEffectivePalletsByDateExcluding(
                    updated.date(), updated.id());
            long slotPallets = appointmentRepository.sumEffectivePalletsByDateAndTimeSlotExcluding(
                    updated.date(), timeSlot.id(), updated.id());
            CapacityAssessment assessment = capacityPolicy.assess(
                    dailyPallets, slotPallets, updated.effectivePallets());

            Appointment saved = appointmentRepository.save(updated);
            auditService.record(
                    command.actorEmail(),
                    AuditAction.UPDATE,
                    "Appointment",
                    saved.id(),
                    "Agendamento atualizado: fornecedor=%s, data=%s, faixa=%s, pallets=%d, efetivos=%d"
                            .formatted(saved.supplierName(), saved.date(), saved.timeSlot().label(),
                                    saved.palletQuantity(), saved.effectivePallets())
            );

            return new PreparedAppointment(saved, assessment);
        });

        CapacityAssessment assessment = prepared.assessment();
        return new AppointmentCommandResult(
                AppointmentResult.from(prepared.appointment()),
                assessment.warnings(),
                CapacityResult.from(assessment)
        );
    }

    private void validate(UpdateAppointmentCommand command) {
        if (command == null) {
            throw new DomainException("Dados do agendamento são obrigatórios.");
        }
        if (command.id() == null) {
            throw new DomainException("Identificador do agendamento é obrigatório.");
        }
        if (command.date() == null) {
            throw new DomainException("Data do agendamento é obrigatória.");
        }
        if (command.timeSlotId() == null || command.truckTypeId() == null
                || command.palletizationTypeId() == null) {
            throw new DomainException("Faixa horária, tipo de caminhão e tipo de paletização são obrigatórios.");
        }
        if (command.actorEmail() == null || command.actorEmail().isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
    }

    private record PreparedAppointment(Appointment appointment, CapacityAssessment assessment) {
    }
}
