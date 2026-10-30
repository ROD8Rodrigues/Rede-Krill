package com.redekrill.scheduling.application.dto.audit;

import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;

import java.time.Instant;

public record AuditLogResult(
        Long id,
        String actorEmail,
        AuditAction action,
        String entityName,
        String entityId,
        String details,
        Instant createdAt
) {

    public static AuditLogResult from(AuditLog auditLog) {
        return new AuditLogResult(
                auditLog.id(),
                auditLog.actorEmail(),
                auditLog.action(),
                auditLog.entityName(),
                auditLog.entityId(),
                auditLog.details(),
                auditLog.createdAt()
        );
    }
}
