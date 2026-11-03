package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateTimeSlotRequest(
        @NotNull(message = "Horário inicial é obrigatório.") LocalTime startTime,
        @NotNull(message = "Horário final é obrigatório.") LocalTime endTime
) {
}
