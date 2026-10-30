package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;

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
