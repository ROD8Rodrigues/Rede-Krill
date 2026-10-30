package com.redekrill.scheduling.presentation.response;

import java.time.Instant;

public record TruckTypeResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
