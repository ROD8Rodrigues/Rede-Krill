package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityResult;
import com.redekrill.scheduling.application.dto.appointment.CreateAppointmentCommand;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.EmailOutboxService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.CapacityAssessment;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class CreateAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TruckTypeRepository truckTypeRepository;
    private final PalletizationTypeRepository palletizationTypeRepository;
    private final UserRepository userRepository;
    private final CapacityPolicy capacityPolicy;
    private final AuditService auditService;
    private final EmailOutboxService emailOutboxService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreateAppointmentUseCase(AppointmentRepository appointmentRepository,
                                    TimeSlotRepository timeSlotRepository,
                                    TruckTypeRepository truckTypeRepository,
                                    PalletizationTypeRepository palletizationTypeRepository,
                                    UserRepository userRepository,
                                    CapacityPolicy capacityPolicy,
                                    AuditService auditService,
                                    EmailOutboxService emailOutboxService,
                                    TransactionRunner transactionRunner,
                                    Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.truckTypeRepository = truckTypeRepository;
        this.palletizationTypeRepository = palletizationTypeRepository;
        this.userRepository = userRepository;
        this.capacityPolicy = capacityPolicy;
        this.auditService = auditService;
        this.emailOutboxService = emailOutboxService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public AppointmentCommandResult execute(CreateAppointmentCommand command) {
        validate(command);

        PreparedAppointment prepared = transactionRunner.execute(() -> {
            TimeSlot timeSlot = timeSlotRepository.findById(command.timeSlotId())
                    .orElseThrow(() -> new ResourceNotFoundException("Faixa horária não encontrada."));
            TruckType truckType = truckTypeRepository.findById(command.truckTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de caminhão não encontrado."));
            PalletizationType palletizationType = palletizationTypeRepository
                    .findById(command.palletizationTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de paletização não encontrado."));

            User actor = userRepository.findByEmail(command.actorEmail())
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário autenticado não encontrado ou inativo."));

            if (appointmentRepository.existsByOrderId(command.orderId())) {
                throw new ConflictException(
                        "Já existe um agendamento com o ID Pedido %d.".formatted(command.orderId()));
            }

            Appointment appointment = Appointment.create(
                    command.orderId(),
                    command.supplierName(),
                    command.supplierEmail(),
                    command.truckPlate(),
                    command.date(),
                    timeSlot,
                    truckType,
                    palletizationType,
                    command.palletQuantity(),
                    command.notes(),
                    actor,
                    Instant.now(clock)
            );

            long dailyPallets = appointmentRepository.sumDailyPalletsByDate(appointment.date());
            long slotPallets = appointmentRepository.sumSlotPalletsByDateAndTimeSlot(
                    appointment.date(), timeSlot.id());
            CapacityAssessment assessment = capacityPolicy.assess(
                    dailyPallets, slotPallets,
                    appointment.dailyPallets(), appointment.slotPallets());

            Appointment saved = appointmentRepository.save(appointment);
            auditService.record(
                    actor.email(),
                    AuditAction.CREATE,
                    "Appointment",
                    saved.id(),
                    "Agendamento criado: ID Pedido=%d, fornecedor=%s, data=%s, faixa=%s, pallets=%d, faixa=%d"
                            .formatted(saved.orderId(), saved.supplierName(), saved.date(),
                                    saved.timeSlot().label(), saved.dailyPallets(), saved.slotPallets())
            );
            emailOutboxService.enqueue(saved, assessment.warnings());

            return new PreparedAppointment(saved, assessment);
        });

        return new AppointmentCommandResult(
                AppointmentResult.from(prepared.appointment()),
                prepared.assessment().warnings(),
                CapacityResult.from(prepared.assessment()),
                true,
                false
        );
    }

    private void validate(CreateAppointmentCommand command) {
        if (command == null) {
            throw new DomainException("Dados do agendamento são obrigatórios.");
        }
        if (command.orderId() == null || command.orderId() <= 0) {
            throw new DomainException("ID Pedido é obrigatório e deve ser maior que zero.");
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
