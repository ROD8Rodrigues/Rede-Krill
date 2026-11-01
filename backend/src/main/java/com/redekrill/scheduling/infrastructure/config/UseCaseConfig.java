package com.redekrill.scheduling.infrastructure.config;

import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.usecase.appointment.CancelAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.CreateAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentReportUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetCapacityOverviewUseCase;
import com.redekrill.scheduling.application.usecase.appointment.ListAppointmentsUseCase;
import com.redekrill.scheduling.application.usecase.appointment.UpdateAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.audit.ListAuditLogsUseCase;
import com.redekrill.scheduling.application.usecase.auth.GetCurrentUserUseCase;
import com.redekrill.scheduling.application.usecase.auth.LoginUseCase;
import com.redekrill.scheduling.application.usecase.auth.LogoutUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListPalletizationTypesUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListTimeSlotsUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.CreateTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.DeleteTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.ListTruckTypesUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.UpdateTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.user.CreateUserUseCase;
import com.redekrill.scheduling.application.usecase.user.ListUsersUseCase;
import com.redekrill.scheduling.application.usecase.user.UpdateUserUseCase;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public CapacityPolicy capacityPolicy() {
        return new CapacityPolicy();
    }

    @Bean
    public AuditService auditService(AuditLogRepository auditLogRepository, Clock clock) {
        return new AuditService(auditLogRepository, clock);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                                     TokenIssuer tokenIssuer, AuditService auditService) {
        return new LoginUseCase(userRepository, passwordHasher, tokenIssuer, auditService);
    }

    @Bean
    public LogoutUseCase logoutUseCase(AuditService auditService) {
        return new LogoutUseCase(auditService);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(UserRepository userRepository) {
        return new GetCurrentUserUseCase(userRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository,
                                               PasswordHasher passwordHasher,
                                               AuditService auditService,
                                               TransactionRunner transactionRunner,
                                               Clock clock) {
        return new CreateUserUseCase(
                userRepository, passwordHasher, auditService, transactionRunner, clock);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository,
                                               PasswordHasher passwordHasher,
                                               AuditService auditService,
                                               TransactionRunner transactionRunner,
                                               Clock clock) {
        return new UpdateUserUseCase(
                userRepository, passwordHasher, auditService, transactionRunner, clock);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersUseCase(userRepository);
    }

    @Bean
    public CreateTruckTypeUseCase createTruckTypeUseCase(TruckTypeRepository truckTypeRepository,
                                                         AuditService auditService,
                                                         TransactionRunner transactionRunner,
                                                         Clock clock) {
        return new CreateTruckTypeUseCase(
                truckTypeRepository, auditService, transactionRunner, clock);
    }

    @Bean
    public UpdateTruckTypeUseCase updateTruckTypeUseCase(TruckTypeRepository truckTypeRepository,
                                                         AuditService auditService,
                                                         TransactionRunner transactionRunner,
                                                         Clock clock) {
        return new UpdateTruckTypeUseCase(
                truckTypeRepository, auditService, transactionRunner, clock);
    }

    @Bean
    public DeleteTruckTypeUseCase deleteTruckTypeUseCase(TruckTypeRepository truckTypeRepository,
                                                         AppointmentRepository appointmentRepository,
                                                         AuditService auditService,
                                                         TransactionRunner transactionRunner) {
        return new DeleteTruckTypeUseCase(
                truckTypeRepository, appointmentRepository, auditService, transactionRunner);
    }

    @Bean
    public ListTruckTypesUseCase listTruckTypesUseCase(TruckTypeRepository truckTypeRepository) {
        return new ListTruckTypesUseCase(truckTypeRepository);
    }

    @Bean
    public ListPalletizationTypesUseCase listPalletizationTypesUseCase(
            PalletizationTypeRepository palletizationTypeRepository) {
        return new ListPalletizationTypesUseCase(palletizationTypeRepository);
    }

    @Bean
    public ListTimeSlotsUseCase listTimeSlotsUseCase(TimeSlotRepository timeSlotRepository) {
        return new ListTimeSlotsUseCase(timeSlotRepository);
    }

    @Bean
    public CreateAppointmentUseCase createAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            TruckTypeRepository truckTypeRepository,
            PalletizationTypeRepository palletizationTypeRepository,
            UserRepository userRepository,
            CapacityPolicy capacityPolicy,
            AuditService auditService,
            EmailNotifier emailNotifier,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new CreateAppointmentUseCase(
                appointmentRepository,
                timeSlotRepository,
                truckTypeRepository,
                palletizationTypeRepository,
                userRepository,
                capacityPolicy,
                auditService,
                emailNotifier,
                transactionRunner,
                clock
        );
    }

    @Bean
    public UpdateAppointmentUseCase updateAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            TruckTypeRepository truckTypeRepository,
            PalletizationTypeRepository palletizationTypeRepository,
            CapacityPolicy capacityPolicy,
            AuditService auditService,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new UpdateAppointmentUseCase(
                appointmentRepository,
                timeSlotRepository,
                truckTypeRepository,
                palletizationTypeRepository,
                capacityPolicy,
                auditService,
                transactionRunner,
                clock
        );
    }

    @Bean
    public CancelAppointmentUseCase cancelAppointmentUseCase(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditService auditService,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new CancelAppointmentUseCase(
                appointmentRepository, userRepository, auditService, transactionRunner, clock);
    }

    @Bean
    public GetAppointmentUseCase getAppointmentUseCase(AppointmentRepository appointmentRepository) {
        return new GetAppointmentUseCase(appointmentRepository);
    }

    @Bean
    public ListAppointmentsUseCase listAppointmentsUseCase(AppointmentRepository appointmentRepository) {
        return new ListAppointmentsUseCase(appointmentRepository);
    }

    @Bean
    public GetCapacityOverviewUseCase getCapacityOverviewUseCase(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository) {
        return new GetCapacityOverviewUseCase(appointmentRepository, timeSlotRepository);
    }

    @Bean
    public GetAppointmentReportUseCase getAppointmentReportUseCase(
            AppointmentRepository appointmentRepository) {
        return new GetAppointmentReportUseCase(appointmentRepository);
    }

    @Bean
    public ListAuditLogsUseCase listAuditLogsUseCase(AuditLogRepository auditLogRepository) {
        return new ListAuditLogsUseCase(auditLogRepository);
    }
}
