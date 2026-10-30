package com.redekrill.scheduling.application.dto.reference;

import com.redekrill.scheduling.domain.model.TimeSlot;

import java.time.LocalTime;

public record TimeSlotResult(
        Long id,
        LocalTime startTime,
        LocalTime endTime
) {

    public static TimeSlotResult from(TimeSlot timeSlot) {
        return new TimeSlotResult(timeSlot.id(), timeSlot.startTime(), timeSlot.endTime());
    }

    public String label() {
        return startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                + " - "
                + endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }
}
