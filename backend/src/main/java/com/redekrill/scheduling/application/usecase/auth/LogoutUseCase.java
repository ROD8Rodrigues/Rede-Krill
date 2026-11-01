package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuditAction;

/**
 * Registra o logout na trilha de auditoria. O token JWT é stateless e deixa de
 * ser aceito quando o cliente o descarta.
 */
public class LogoutUseCase {

    private final AuditService auditService;

    public LogoutUseCase(AuditService auditService) {
        this.auditService = auditService;
    }

    public void execute(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
        auditService.record(actorEmail, AuditAction.LOGOUT, "User", null, "Logout realizado.");
    }
}
