package com.redekrill.scheduling.application.dto.reference;

public record CreatePalletizationTypeCommand(String name, Integer capacityMultiplier,
                                             String actorEmail) {
}
