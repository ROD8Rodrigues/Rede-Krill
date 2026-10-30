package com.redekrill.scheduling.application.dto.trucktype;

import com.redekrill.scheduling.domain.model.TruckType;

import java.time.Instant;

public record TruckTypeResult(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static TruckTypeResult from(TruckType truckType) {
        return new TruckTypeResult(
                truckType.id(),
                truckType.name(),
                truckType.description(),
                truckType.createdAt(),
                truckType.updatedAt()
        );
    }
}
