package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository, Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsAuditLogWithEntityId() {
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record("Admin@RedeKrill.com", AuditAction.CREATE, "Appointment", 42L, "detalhes");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.actorEmail()).isEqualTo("admin@redekrill.com");
        assertThat(saved.action()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.entityName()).isEqualTo("Appointment");
        assertThat(saved.entityId()).isEqualTo("42");
        assertThat(saved.details()).isEqualTo("detalhes");
        assertThat(saved.createdAt()).isEqualTo(TestFixtures.NOW);
    }

    @Test
    void recordsAuditLogWithoutEntityIdAndDetails() {
        when(auditLogRepository.save(org.mockito.ArgumentMatchers.any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record("admin@redekrill.com", AuditAction.DELETE, "User", null, "  ");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().entityId()).isNull();
        assertThat(captor.getValue().details()).isNull();
    }
}
