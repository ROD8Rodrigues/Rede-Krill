package com.redekrill.scheduling.application.usecase.audit;

import com.redekrill.scheduling.application.dto.audit.AuditLogResult;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAuditLogsUseCaseTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private ListAuditLogsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAuditLogsUseCase(auditLogRepository);
    }

    @Test
    void listsAuditLogsWithNormalizedPagination() {
        AuditLog auditLog = AuditLog.create(
                "admin@redekrill.com", AuditAction.CREATE, "Appointment", "1", "criado",
                TestFixtures.NOW).withId(9L);
        when(auditLogRepository.findAll(0, 100))
                .thenReturn(new PageResult<>(List.of(auditLog), 0, 100, 1, 1));

        PageResult<AuditLogResult> result = useCase.execute(-5, 500);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(9L);
        assertThat(result.content().getFirst().action()).isEqualTo(AuditAction.CREATE);
        verify(auditLogRepository).findAll(0, 100);
    }
}
