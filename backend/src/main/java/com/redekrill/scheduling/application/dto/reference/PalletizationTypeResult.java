package com.redekrill.scheduling.application.dto.reference;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;

public record PalletizationTypeResult(
        Long id,
        PalletizationCode code,
        String name,
        int capacityMultiplier
) {

    public static PalletizationTypeResult from(PalletizationType type) {
        return new PalletizationTypeResult(
                type.id(),
                type.code(),
                type.name(),
                type.capacityMultiplier()
        );
    }
}
