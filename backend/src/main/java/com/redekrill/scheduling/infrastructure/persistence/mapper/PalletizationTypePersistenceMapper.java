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
}
