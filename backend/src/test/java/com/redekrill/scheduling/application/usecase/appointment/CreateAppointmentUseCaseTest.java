package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.CreateAppointmentCommand;
import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private TruckTypeRepository truckTypeRepository;
    @Mock
    private PalletizationTypeRepository palletizationTypeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailNotifier emailNotifier;

    private CreateAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAppointmentUseCase(
                appointmentRepository,
                timeSlotRepository,
                truckTypeRepository,
                palletizationTypeRepository,
                userRepository,
                new CapacityPolicy(),
                auditService,
                emailNotifier,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsAppointmentAndWarnsWithoutBlockingWhenDailyLimitIsExceeded() {
        stubReferences(TestFixtures.nonPalletized());
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(appointmentRepository.sumEffectivePalletsByDate(TestFixtures.DATE)).thenReturn(470L);
        when(appointmentRepository.sumEffectivePalletsByDateAndTimeSlot(TestFixtures.DATE, 1L)).thenReturn(90L);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Appointment.class).withId(10L));

        AppointmentCommandResult result = useCase.execute(nonPalletizedCommand(10));

        assertThat(result.appointment().id()).isEqualTo(10L);
        assertThat(result.appointment().effectivePallets()).isEqualTo(20);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().getFirst()).contains("Limite diário");
        assertThat(result.capacity().dailyPallets()).isEqualTo(490);
        assertThat(result.capacity().slotPallets()).isEqualTo(110);

        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.CREATE),
                eq("Appointment"),
                eq(10L),
                contains("pallets=10")
        );
        verify(emailNotifier).sendAppointmentConfirmation(any(Appointment.class), eq(result.warnings()));
    }

    @Test
    void warnsForBothLimitsWithoutBlocking() {
        stubReferences(TestFixtures.nonPalletized());
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(appointmentRepository.sumEffectivePalletsByDate(TestFixtures.DATE)).thenReturn(480L);
        when(appointmentRepository.sumEffectivePalletsByDateAndTimeSlot(TestFixtures.DATE, 1L)).thenReturn(120L);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Appointment.class).withId(11L));

        AppointmentCommandResult result = useCase.execute(nonPalletizedCommand(1));

        assertThat(result.warnings()).hasSize(2);
        assertThat(result.capacity().dailyPallets()).isEqualTo(482);
        assertThat(result.capacity().slotPallets()).isEqualTo(122);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void keepsAppointmentWhenEmailDeliveryFails() {
        stubReferences(TestFixtures.palletized());
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Appointment.class).withId(12L));
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(emailNotifier).sendAppointmentConfirmation(any(Appointment.class), any());

        AppointmentCommandResult result = useCase.execute(palletizedCommand(5));

        assertThat(result.appointment().id()).isEqualTo(12L);
        assertThat(result.appointment().effectivePallets()).isEqualTo(5);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditService).record(eq("admin@redekrill.com"), eq(AuditAction.CREATE),
                eq("Appointment"), eq(12L), any());
    }

    @Test
    void rejectsMissingCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Dados do agendamento são obrigatórios.");
    }

    @Test
    void rejectsMissingDate() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", null,
                1L, 1L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data do agendamento é obrigatória.");
    }

    @Test
    void rejectsMissingReferences() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                null, 1L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Faixa horária");
    }

    @Test
    void rejectsMissingActor() {
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 1L, 10, "  ");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não identificado.");
    }

    @Test
    void rejectsUnknownTimeSlot() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(commandWithTimeSlot(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Faixa horária não encontrada.");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownTruckType() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.timeSlot()));
        when(truckTypeRepository.findById(99L)).thenReturn(Optional.empty());

        CreateAppointmentCommand command = new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 99L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de caminhão não encontrado.");
    }

    @Test
    void rejectsUnknownPalletizationType() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.timeSlot()));
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(palletizationTypeRepository.findById(99L)).thenReturn(Optional.empty());

        CreateAppointmentCommand command = new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 99L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de paletização não encontrado.");
    }

    @Test
    void rejectsInactiveActor() {
        stubReferences(TestFixtures.palletized());
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.inactiveUser()));

        assertThatThrownBy(() -> useCase.execute(palletizedCommand(10)))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não encontrado ou inativo.");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownActor() {
        stubReferences(TestFixtures.palletized());
        when(userRepository.findByEmail("admin@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(palletizedCommand(10)))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    private void stubReferences(com.redekrill.scheduling.domain.model.PalletizationType palletizationType) {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.timeSlot()));
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(palletizationTypeRepository.findById(palletizationType.id()))
                .thenReturn(Optional.of(palletizationType));
    }

    private CreateAppointmentCommand nonPalletizedCommand(int quantity) {
        return new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 2L, quantity, "admin@redekrill.com");
    }

    private CreateAppointmentCommand palletizedCommand(int quantity) {
        return new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 1L, quantity, "admin@redekrill.com");
    }

    private CreateAppointmentCommand commandWithTimeSlot(Long timeSlotId) {
        return new CreateAppointmentCommand(
                "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                timeSlotId, 1L, 1L, 10, "admin@redekrill.com");
    }
}
