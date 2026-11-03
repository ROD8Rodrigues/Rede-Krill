package com.redekrill.scheduling.application.dto.reference;

import java.time.LocalTime;

public record CreateTimeSlotCommand(LocalTime startTime, LocalTime endTime, String actorEmail) {
}
