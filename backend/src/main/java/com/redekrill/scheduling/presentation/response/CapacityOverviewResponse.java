package com.redekrill.scheduling.presentation.response;

import java.time.LocalDate;
import java.util.List;

public record CapacityOverviewResponse(
        LocalDate date,
        long dailyPallets,
        int dailyLimit,
        double dailyOccupancyPercentage,
        List<SlotCapacityResponse> slots
) {

    public CapacityOverviewResponse {
        slots = List.copyOf(slots);
    }
}
