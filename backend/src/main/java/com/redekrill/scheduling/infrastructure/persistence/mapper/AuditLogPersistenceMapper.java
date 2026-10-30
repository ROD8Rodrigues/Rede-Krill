package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.infrastructure.persistence.entity.AuditLogJpaEntity;

public final class AuditLogPersistenceMapper {

    private AuditLogPersistenceMapper() {
    }

    public static AuditLog toDomain(AuditLogJpaEntity entity) {
        return new AuditLog(
                entity.getId(),
                entity.getActorEmail(),
                entity.getAction(),
                entity.getEntityName(),
                entity.getEntityId(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }

    public static AuditLogJpaEntity toEntity(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(auditLog.id());
        entity.setActorEmail(auditLog.actorEmail());
        entity.setAction(auditLog.action());
        entity.setEntityName(auditLog.entityName());
        entity.setEntityId(auditLog.entityId());
        entity.setDetails(auditLog.details());
        entity.setCreatedAt(auditLog.createdAt());
        return entity;
    }
}
