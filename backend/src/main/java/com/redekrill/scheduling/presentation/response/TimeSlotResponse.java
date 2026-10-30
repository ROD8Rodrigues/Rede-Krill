package com.redekrill.scheduling.presentation.response;

public record TimeSlotResponse(
        Long id,
        String startTime,
        String endTime,
        String label
) {
}
