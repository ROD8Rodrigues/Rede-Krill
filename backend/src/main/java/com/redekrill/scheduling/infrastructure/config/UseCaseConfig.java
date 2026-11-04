/*
 * O único lugar onde caso de uso e Spring se encontram.
 *
 * Os casos de uso são classes planas: sem @Service, sem @Component, sem nada do
 * framework. Isso não é purismo — é o que permite instanciá-los num teste unitário com
 * falsos nos ports e nenhum contexto carregado. O preço é alguém ter que escrever o
 * "new" de cada um, e esse alguém é este arquivo: os 33 beans de aplicação e domínio
 * montados à mão. Os seis que faltam estão espalhados por SecurityConfig (3), ClockConfig,
 * SecurityBeansConfig e OpenApiConfig — cada um produzindo objeto de infraestrutura, não
 * de caso de uso.
 *
 * A convenção é sempre a mesma: cada bean recebe pelo construtor as interfaces de que
 * precisa — repositórios, ports, policies, Clock, propriedades — e o Spring entrega as
 * implementações que vivem em infrastructure. Não há uma linha de condição ou laço aqui:
 * se uma montagem pedisse um if, essa decisão já seria regra de negócio e teria que
 * subir para um serviço de aplicação.
 *
 * Quando um caso de uso novo aparecer, é aqui que ele nasce e é aqui que a dependência
 * nova aparece primeiro. Se ela não couber em uma linha de new, a arquitetura pediu algo.
 */
package com.redekrill.scheduling.infrastructure.config;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.RefreshTokenGenerator;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.application.service.EmailOutboxService;
import com.redekrill.scheduling.application.service.LoginAttemptService;
import com.redekrill.scheduling.application.usecase.appointment.CancelAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.CreateAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentReportUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetCapacityOverviewUseCase;
import com.redekrill.scheduling.application.usecase.appointment.ListAppointmentsUseCase;
import com.redekrill.scheduling.application.usecase.appointment.UpdateAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.audit.ListAuditLogsUseCase;
import com.redekrill.scheduling.application.usecase.auth.ChangePasswordUseCase;
import com.redekrill.scheduling.application.usecase.auth.GetCurrentUserUseCase;
import com.redekrill.scheduling.application.usecase.auth.LoginUseCase;
import com.redekrill.scheduling.application.usecase.auth.LogoutUseCase;
import com.redekrill.scheduling.application.usecase.reference.CreatePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.CreateTimeSlotUseCase;
import com.redekrill.scheduling.application.usecase.reference.DeletePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.DeleteTimeSlotUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListPalletizationTypesUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListTimeSlotsUseCase;
import com.redekrill.scheduling.application.usecase.reference.UpdatePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.UpdateTimeSlotUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.CreateTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.DeleteTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.ListTruckTypesUseCase;
import com.redekrill.scheduling.application.usecase.trucktype.UpdateTruckTypeUseCase;
import com.redekrill.scheduling.application.usecase.user.CreateUserUseCase;
import com.redekrill.scheduling.application.usecase.user.ListUsersUseCase;
import com.redekrill.scheduling.application.usecase.user.UpdateUserUseCase;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Montagem manual dos beans de aplicação. Não contém regra de negócio: só decide quais
 * implementações entram nos construtores dos casos de uso.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public CapacityPolicy capacityPolicy() {
        return new CapacityPolicy();
    }

    @Bean
    public PasswordPolicy passwordPolicy() {
        return new PasswordPolicy();
    }

    @Bean
    public AuditService auditService(AuditLogRepository auditLogRepository, Clock clock) {
        return new AuditService(auditLogRepository, clock);
    }

    @Bean
    public LoginAttemptService loginAttemptService(Clock clock, SecurityProperties properties) {
        return new LoginAttemptService(
                clock,
                properties.loginMaxAttemptsPerAccount(),
                properties.loginMaxAttemptsPerIp(),
                properties.loginWindow(),
                properties.loginLockout()
        );
    }

    @Bean
    public AuthSessionService authSessionService(AuthSessionRepository authSessionRepository,
                                                 UserRepository userRepository,
                                                 TokenIssuer tokenIssuer,
                                                 RefreshTokenGenerator refreshTokenGenerator,
                                                 TransactionRunner transactionRunner,
                                                 Clock clock,
                                                 AuthProperties authProperties,
                                                 AuditService auditService) {
        return new AuthSessionService(
                authSessionRepository,
                userRepository,
                tokenIssuer,
                refreshTokenGenerator,
                transactionRunner,
                clock,
                authProperties.refreshExpiration(),
                auditService
        );
    }

    @Bean
    public EmailOutboxService emailOutboxService(EmailOutboxRepository repository, Clock clock) {
        return new EmailOutboxService(repository, clock);
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository,
                                     PasswordHasher passwordHasher,
                                     AuditService auditService,
                                     LoginAttemptService loginAttemptService,
                                     AuthSessionService authSessionService) {
        return new LoginUseCase(userRepository, passwordHasher, auditService,
                loginAttemptService, authSessionService);
    }

    @Bean
    public LogoutUseCase logoutUseCase(AuditService auditService) {
        return new LogoutUseCase(auditService);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase(UserRepository userRepository,
                                                       PasswordHasher passwordHasher,
                                                       AuthSessionService authSessionService,
                                                       AuditService auditService,
                                                       TransactionRunner transactionRunner,
                                                       Clock clock,
                                                       PasswordPolicy passwordPolicy) {
        return new ChangePasswordUseCase(userRepository, passwordHasher, authSessionService,
                auditService, transactionRunner, clock, passwordPolicy);
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
                                               Clock clock,
                                               PasswordPolicy passwordPolicy) {
        return new CreateUserUseCase(
                userRepository, passwordHasher, auditService, transactionRunner, clock,
                passwordPolicy);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository,
                                               PasswordHasher passwordHasher,
                                               AuditService auditService,
                                               TransactionRunner transactionRunner,
                                               AuthSessionService authSessionService,
                                               Clock clock,
                                               PasswordPolicy passwordPolicy) {
        return new UpdateUserUseCase(
                userRepository, passwordHasher, auditService, transactionRunner,
                authSessionService, clock, passwordPolicy);
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
    public CreateTimeSlotUseCase createTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                                       AuditService auditService,
                                                       TransactionRunner transactionRunner) {
        return new CreateTimeSlotUseCase(timeSlotRepository, auditService, transactionRunner);
    }

    @Bean
    public UpdateTimeSlotUseCase updateTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                                       AuditService auditService,
                                                       TransactionRunner transactionRunner) {
        return new UpdateTimeSlotUseCase(timeSlotRepository, auditService, transactionRunner);
    }

    @Bean
    public DeleteTimeSlotUseCase deleteTimeSlotUseCase(TimeSlotRepository timeSlotRepository,
                                                       AppointmentRepository appointmentRepository,
                                                       AuditService auditService,
                                                       TransactionRunner transactionRunner) {
        return new DeleteTimeSlotUseCase(
                timeSlotRepository, appointmentRepository, auditService, transactionRunner);
    }

    @Bean
    public CreatePalletizationTypeUseCase createPalletizationTypeUseCase(
            PalletizationTypeRepository repository,
            AuditService auditService,
            TransactionRunner transactionRunner) {
        return new CreatePalletizationTypeUseCase(repository, auditService, transactionRunner);
    }

    @Bean
    public UpdatePalletizationTypeUseCase updatePalletizationTypeUseCase(
            PalletizationTypeRepository repository,
            AuditService auditService,
            TransactionRunner transactionRunner) {
        return new UpdatePalletizationTypeUseCase(repository, auditService, transactionRunner);
    }

    @Bean
    public DeletePalletizationTypeUseCase deletePalletizationTypeUseCase(
            PalletizationTypeRepository repository,
            AppointmentRepository appointmentRepository,
            AuditService auditService,
            TransactionRunner transactionRunner) {
        return new DeletePalletizationTypeUseCase(
                repository, appointmentRepository, auditService, transactionRunner);
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
            EmailOutboxService emailOutboxService,
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
                emailOutboxService,
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
            EmailOutboxService emailOutboxService,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new CancelAppointmentUseCase(
                appointmentRepository, userRepository, auditService, emailOutboxService,
                transactionRunner, clock);
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
