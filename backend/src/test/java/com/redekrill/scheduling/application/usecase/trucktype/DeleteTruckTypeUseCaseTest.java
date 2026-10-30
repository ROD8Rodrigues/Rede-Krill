package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteTruckTypeUseCaseTest {

    @Mock
    private TruckTypeRepository truckTypeRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AuditService auditService;

    private DeleteTruckTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTruckTypeUseCase(
                truckTypeRepository, appointmentRepository, auditService,
                new ImmediateTransactionRunner());
    }

    @Test
    void deletesTruckTypeAndRecordsAudit() {
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(appointmentRepository.existsByTruckTypeId(1L)).thenReturn(false);

        useCase.execute(1L, "admin@redekrill.com");

        verify(truckTypeRepository).deleteById(1L);
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.DELETE),
                eq("TruckType"),
                eq(1L),
                contains("Carreta")
        );
    }

    @Test
    void rejectsUnknownTruckType() {
        when(truckTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, "admin@redekrill.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de caminhão não encontrado.");
    }

    @Test
    void rejectsTruckTypeInUse() {
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(appointmentRepository.existsByTruckTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(1L, "admin@redekrill.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Tipo de caminhão em uso por um agendamento e não pode ser excluído.");
        verify(truckTypeRepository, never()).deleteById(anyLong());
    }
}
