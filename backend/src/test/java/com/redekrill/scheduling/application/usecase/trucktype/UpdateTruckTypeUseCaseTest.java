package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.dto.trucktype.UpdateTruckTypeCommand;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.TruckType;
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
class UpdateTruckTypeUseCaseTest {

    @Mock
    private TruckTypeRepository truckTypeRepository;
    @Mock
    private AuditService auditService;

    private UpdateTruckTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTruckTypeUseCase(
                truckTypeRepository, auditService, new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void updatesTruckTypeAndRecordsAudit() {
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(truckTypeRepository.save(any(TruckType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, TruckType.class));

        TruckTypeResult result = useCase.execute(new UpdateTruckTypeCommand(
                1L, "Carreta LS", "Nova descrição", "admin@redekrill.com"));

        assertThat(result.name()).isEqualTo("Carreta LS");
        assertThat(result.description()).isEqualTo("Nova descrição");
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.UPDATE),
                eq("TruckType"),
                eq(1L),
                contains("Carreta LS")
        );
    }

    @Test
    void rejectsUnknownTruckType() {
        when(truckTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateTruckTypeCommand(
                99L, "Carreta", null, "admin@redekrill.com")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tipo de caminhão não encontrado.");
    }

    @Test
    void rejectsDuplicateName() {
        when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.truckType()));
        when(truckTypeRepository.existsByNameAndIdNot("Bitrem", 1L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateTruckTypeCommand(
                1L, "Bitrem", null, "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class);
        verify(truckTypeRepository, never()).save(any());
    }
}
