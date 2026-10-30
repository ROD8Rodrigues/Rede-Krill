package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

public record AuditLog(
        Long id,
        String actorEmail,
        AuditAction action,
        String entityName,
        String entityId,
        String details,
        Instant createdAt
) {

    public AuditLog {
        actorEmail = DomainValidation.requireEmail(actorEmail, "E-mail do autor é obrigatório.");
        Objects.requireNonNull(action, "Ação de auditoria é obrigatória.");
        entityName = DomainValidation.requireText(entityName, "Nome da entidade é obrigatório.");
        entityId = entityId == null || entityId.isBlank() ? null : entityId.trim();
        details = details == null || details.isBlank() ? null : details.trim();
        Objects.requireNonNull(createdAt, "Data do log é obrigatória.");
    }

    public static AuditLog create(String actorEmail, AuditAction action, String entityName,
                                  String entityId, String details, Instant now) {
        return new AuditLog(null, actorEmail, action, entityName, entityId, details, now);
    }

    public AuditLog withId(Long newId) {
        return new AuditLog(newId, actorEmail, action, entityName, entityId, details, createdAt);
    }
}
