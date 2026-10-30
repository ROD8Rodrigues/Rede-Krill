package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.AuditAction;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        AuditAction action,
        String entityName,
        String entityId,
        String details,
        Instant createdAt
) {
}
