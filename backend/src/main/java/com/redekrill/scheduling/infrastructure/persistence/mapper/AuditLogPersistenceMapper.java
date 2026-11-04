/*
 * Anúncia de auditoria vai e volta, campo por campo.
 *
 * É o mapper mais direto do pacote: as duas representações têm exatamente os mesmos
 * sete campos, porque o log é gravado pronto e nunca é reescrito. Não há versão
 * otimista nem relação a resolver — e não há conversão de tipo: entityId já é texto
 * nos dois lados, decisão tomada na escrita pelo AuditService.
 *
 * toEntity cria a instância com o setter de id a partir do modelo. Isso é o que
 * permite ao adaptador devolver auditLog.withId(saved.getId()) em vez de mapear a
 * entity salva de volta — a linha gravada é a que a aplicação já tinha.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.infrastructure.persistence.entity.AuditLogJpaEntity;

/**
 * Conversão entre AuditLogJpaEntity e o modelo AuditLog.
 */
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
