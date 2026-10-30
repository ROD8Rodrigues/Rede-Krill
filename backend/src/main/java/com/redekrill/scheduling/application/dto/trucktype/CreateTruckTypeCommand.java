package com.redekrill.scheduling.application.dto.trucktype;

public record CreateTruckTypeCommand(
        String name,
        String description,
        String actorEmail
) {
}
