package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record TimeSlot(
        Long id,
        LocalTime startTime,
        LocalTime endTime
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public TimeSlot {
        Objects.requireNonNull(startTime, "Horário inicial é obrigatório.");
        Objects.requireNonNull(endTime, "Horário final é obrigatório.");
        if (!startTime.isBefore(endTime)) {
            throw new DomainException("Horário inicial deve ser anterior ao horário final.");
        }
        if (!Duration.between(startTime, endTime).equals(Duration.ofHours(2))) {
            throw new DomainException("Cada faixa horária deve ter exatamente 2 horas.");
        }
    }

    public String label() {
        return startTime.format(TIME_FORMAT) + " - " + endTime.format(TIME_FORMAT);
    }
}
