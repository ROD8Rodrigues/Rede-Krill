package com.redekrill.scheduling.application.dto.trucktype;

public record UpdateTruckTypeCommand(
        Long id,
        String name,
        String description,
        String actorEmail
) {
}
