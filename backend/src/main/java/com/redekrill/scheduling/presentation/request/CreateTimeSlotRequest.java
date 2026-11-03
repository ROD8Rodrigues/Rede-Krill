package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateTimeSlotRequest(
        @NotNull(message = "Horário inicial é obrigatório.") LocalTime startTime,
        @NotNull(message = "Horário final é obrigatório.") LocalTime endTime
) {
}
