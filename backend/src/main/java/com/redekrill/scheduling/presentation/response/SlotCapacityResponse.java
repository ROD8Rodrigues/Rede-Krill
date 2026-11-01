package com.redekrill.scheduling.presentation.response;

public record SlotCapacityResponse(
        TimeSlotResponse timeSlot,
        long pallets,
        int limit,
        double occupancyPercentage
) {
}
