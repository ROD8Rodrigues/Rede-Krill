package com.redekrill.scheduling.domain.policy;

import java.util.List;

public record CapacityAssessment(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit,
        List<String> warnings
) {

    public CapacityAssessment {
        warnings = List.copyOf(warnings);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public double dailyOccupancyPercentage() {
        return CapacityPolicy.occupancyPercentage(dailyPallets, dailyLimit);
    }

    public double slotOccupancyPercentage() {
        return CapacityPolicy.occupancyPercentage(slotPallets, slotLimit);
    }
}
