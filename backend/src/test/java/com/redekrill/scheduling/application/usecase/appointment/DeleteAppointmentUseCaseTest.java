package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AuditService auditService;

    private DeleteAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteAppointmentUseCase(
                appointmentRepository, auditService, new ImmediateTransactionRunner());
    }

    @Test
    void deletesAppointmentAndRecordsAudit() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));

        useCase.execute(1L, "admin@redekrill.com");

        verify(appointmentRepository).deleteById(1L);
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.DELETE),
                eq("Appointment"),
                eq(1L),
                contains("Fornecedor Krill")
        );
    }

    @Test
    void rejectsMissingId() {
        assertThatThrownBy(() -> useCase.execute(null, "admin@redekrill.com"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Identificador do agendamento é obrigatório.");
        verify(appointmentRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownAppointment() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, "admin@redekrill.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado.");
        verify(appointmentRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    }
}
