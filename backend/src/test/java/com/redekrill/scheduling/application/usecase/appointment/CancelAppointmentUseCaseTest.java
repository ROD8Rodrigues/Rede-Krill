package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
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
class CancelAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private CancelAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelAppointmentUseCase(
                appointmentRepository,
                userRepository,
                auditService,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void cancelsLogicalAppointmentAndRecordsAudit() {
        Appointment appointment = TestFixtures.appointment();
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Appointment.class));

        var result = useCase.execute(1L, "admin@redekrill.com");

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(result.canceledBy().email()).isEqualTo("admin@redekrill.com");
        assertThat(result.canceledAt()).isEqualTo(TestFixtures.NOW);
        assertThat(result.orderId()).isEqualTo(appointment.orderId());
        verify(appointmentRepository).save(any(Appointment.class));
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.CANCEL),
                eq("Appointment"),
                eq(1L),
                contains("ID Pedido=")
        );
    }

    @Test
    void rejectsMissingId() {
        assertThatThrownBy(() -> useCase.execute(null, "admin@redekrill.com"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Identificador do agendamento é obrigatório.");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsMissingActor() {
        assertThatThrownBy(() -> useCase.execute(1L, " "))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não identificado.");
    }

    @Test
    void rejectsUnknownAppointment() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, "admin@redekrill.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado.");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownActor() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));
        when(userRepository.findByEmail("admin@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(1L, "admin@redekrill.com"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não encontrado ou inativo.");
    }

    @Test
    void rejectsRepeatedCancellation() {
        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(TestFixtures.canceledAppointment()));
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> useCase.execute(1L, "admin@redekrill.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Agendamento já está cancelado.");
        verify(appointmentRepository, never()).save(any());
    }
}
