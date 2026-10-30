package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.UpdateAppointmentCommand;
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
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private TruckTypeRepository truckTypeRepository;
    @Mock
    private PalletizationTypeRepository palletizationTypeRepository;
    @Mock
    private AuditService auditService;

    private UpdateAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAppointmentUseCase(
                appointmentRepository,
                timeSlotRepository,
                truckTypeRepository,
                palletizationTypeRepository,
                new CapacityPolicy(),
                auditService,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void updatesAppointmentRecalculatingEffectivePalletsAndWarnings() {
        Appointment existing = TestFixtures.appointment();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timeSlotRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.otherTimeSlot()));
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(palletizationTypeRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.nonPalletized()));
        when(appointmentRepository.sumEffectivePalletsByDateExcluding(TestFixtures.DATE, 1L))
                .thenReturn(470L);
        when(appointmentRepository.sumEffectivePalletsByDateAndTimeSlotExcluding(
                TestFixtures.DATE, 2L, 1L)).thenReturn(20L);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Appointment.class));

        AppointmentCommandResult result = useCase.execute(updateCommand(6));

        assertThat(result.appointment().effectivePallets()).isEqualTo(12);
        assertThat(result.appointment().supplierName()).isEqualTo("Fornecedor Atualizado");
        assertThat(result.appointment().timeSlot().id()).isEqualTo(2L);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.capacity().dailyPallets()).isEqualTo(482);
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.UPDATE),
                eq("Appointment"),
                eq(1L),
                contains("pallets=6")
        );
    }

    @Test
    void rejectsMissingCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Dados do agendamento são obrigatórios.");
    }

    @Test
    void rejectsMissingId() {
        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                null, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessage("Identificador do agendamento é obrigatório.");
    }

    @Test
    void rejectsMissingDate() {
        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", null,
                1L, 1L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data do agendamento é obrigatória.");
    }

    @Test
    void rejectsMissingReferences() {
        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, null, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Faixa horária");
    }

    @Test
    void rejectsMissingActor() {
        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 1L, 10, "");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não identificado.");
    }

    @Test
    void rejectsUnknownAppointment() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                99L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado.");
    }

    @Test
    void rejectsUnknownTimeSlot() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(updateCommandWithTimeSlot(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Faixa horária não encontrada.");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownTruckType() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.timeSlot()));
        when(truckTypeRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 99L, 1L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de caminhão não encontrado.");
    }

    @Test
    void rejectsUnknownPalletizationType() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.timeSlot()));
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(palletizationTypeRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateAppointmentCommand command = new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                1L, 1L, 99L, 10, "admin@redekrill.com");

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de paletização não encontrado.");
    }

    private UpdateAppointmentCommand updateCommand(int quantity) {
        return new UpdateAppointmentCommand(
                1L, "Fornecedor Atualizado", "fornecedor@example.com", "ABC1D23",
                TestFixtures.DATE, 2L, 1L, 2L, quantity, "admin@redekrill.com");
    }

    private UpdateAppointmentCommand updateCommandWithTimeSlot(Long timeSlotId) {
        return new UpdateAppointmentCommand(
                1L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                timeSlotId, 1L, 1L, 10, "admin@redekrill.com");
    }
}
