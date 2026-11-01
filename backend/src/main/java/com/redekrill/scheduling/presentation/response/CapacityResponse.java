package com.redekrill.scheduling.presentation.response;

public record CapacityResponse(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit,
        double dailyOccupancyPercentage,
        double slotOccupancyPercentage
) {
}
