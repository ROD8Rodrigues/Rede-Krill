package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;

public final class PalletizationTypePersistenceMapper {

    private PalletizationTypePersistenceMapper() {
    }

    public static PalletizationType toDomain(PalletizationTypeJpaEntity entity) {
        return new PalletizationType(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCapacityMultiplier()
        );
    }

    public static PalletizationTypeJpaEntity toEntity(PalletizationType type) {
        PalletizationTypeJpaEntity entity = new PalletizationTypeJpaEntity();
        entity.setId(type.id());
        entity.setCode(type.code());
        entity.setName(type.name());
        entity.setCapacityMultiplier(type.capacityMultiplier());
        return entity;
    }
}
