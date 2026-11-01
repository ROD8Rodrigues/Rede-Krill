package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private AuditService auditService;

    private LogoutUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUseCase(auditService);
    }

    @Test
    void recordsLogoutAudit() {
        useCase.execute("admin@redekrill.com");

        verify(auditService).record(
                "admin@redekrill.com", AuditAction.LOGOUT, "User", null, "Logout realizado.");
    }

    @Test
    void rejectsMissingActor() {
        assertThatThrownBy(() -> useCase.execute(" "))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não identificado.");
    }
}
