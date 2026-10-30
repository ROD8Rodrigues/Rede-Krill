package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.CreateTruckTypeCommand;
import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTruckTypeUseCaseTest {

    @Mock
    private TruckTypeRepository truckTypeRepository;
    @Mock
    private AuditService auditService;

    private CreateTruckTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTruckTypeUseCase(
                truckTypeRepository, auditService, new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTruckTypeAndRecordsAudit() {
        when(truckTypeRepository.save(any(TruckType.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, TruckType.class).withId(7L));

        TruckTypeResult result = useCase.execute(new CreateTruckTypeCommand(
                "Bitrem", "Caminhão bitrem", "admin@redekrill.com"));

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Bitrem");
        assertThat(result.description()).isEqualTo("Caminhão bitrem");
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.CREATE),
                eq("TruckType"),
                eq(7L),
                contains("Bitrem")
        );
    }

    @Test
    void rejectsDuplicateName() {
        when(truckTypeRepository.existsByName("Carreta")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateTruckTypeCommand(
                "Carreta", "descrição", "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um tipo de caminhão com este nome.");
        verify(truckTypeRepository, never()).save(any());
    }
}
