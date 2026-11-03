package com.redekrill.scheduling.application.dto.reference;

import java.time.LocalTime;

public record UpdateTimeSlotCommand(Long id, LocalTime startTime, LocalTime endTime,
                                    String actorEmail) {
}
