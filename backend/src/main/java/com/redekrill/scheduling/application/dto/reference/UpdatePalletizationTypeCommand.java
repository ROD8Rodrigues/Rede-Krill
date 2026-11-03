package com.redekrill.scheduling.application.dto.reference;

public record UpdatePalletizationTypeCommand(Long id, String name, Integer capacityMultiplier,
                                             String actorEmail) {
}
