package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.PalletizationCode;

public record PalletizationTypeResponse(
        Long id,
        PalletizationCode code,
        String name,
        int capacityMultiplier
) {
}
