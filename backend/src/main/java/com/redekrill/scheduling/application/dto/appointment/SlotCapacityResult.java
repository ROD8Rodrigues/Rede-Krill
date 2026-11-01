package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;

public record SlotCapacityResult(
        TimeSlotResult timeSlot,
        long pallets,
        int limit,
        double occupancyPercentage
) {
}
