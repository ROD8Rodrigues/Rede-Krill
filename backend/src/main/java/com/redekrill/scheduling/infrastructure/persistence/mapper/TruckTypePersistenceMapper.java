/*
 * Tipo de caminhão entre tabela e modelo.
 *
 * Os cinco campos passam diretos, inclusive os dois carimbos de tempo: createdAt e
 * updatedAt pertencem ao modelo, e quem decide o instante da mudança é o caso de uso
 * com o Clock injetado. Nada aqui delega a data ao JPA (nem @PreUpdate nem
 * @CreationTimestamp), senão a hora da entidade e a hora do log de auditoria viriam
 * de relógios diferentes.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;

/**
 * Conversão entre TruckTypeJpaEntity e o modelo TruckType.
 */
public final class TruckTypePersistenceMapper {

    private TruckTypePersistenceMapper() {
    }

    public static TruckType toDomain(TruckTypeJpaEntity entity) {
        return new TruckType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static TruckTypeJpaEntity toEntity(TruckType truckType) {
        TruckTypeJpaEntity entity = new TruckTypeJpaEntity();
        entity.setId(truckType.id());
        entity.setName(truckType.name());
        entity.setDescription(truckType.description());
        entity.setCreatedAt(truckType.createdAt());
        entity.setUpdatedAt(truckType.updatedAt());
        return entity;
    }
}
