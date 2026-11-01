package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;
import java.util.List;

public record CapacityOverviewResult(
        LocalDate date,
        long dailyPallets,
        int dailyLimit,
        double dailyOccupancyPercentage,
        List<SlotCapacityResult> slots
) {

    public CapacityOverviewResult {
        slots = List.copyOf(slots);
    }
}
