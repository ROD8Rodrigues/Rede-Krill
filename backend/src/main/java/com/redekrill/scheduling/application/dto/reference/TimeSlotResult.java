/*
 * Uma faixa horária como ela é devolvida, com o rótulo pronto.
 *
 * label() repete o formato "HH:mm - HH:mm" que já existe no modelo TimeSlot. Parece
 * duplicação, e é uma escolha: o cliente recebe a faixa como texto de exibição sem
 * precisar concatenar horários, e os dois lugares onde o formato importa — a lista na
 * tela e o corpo do e-mail de confirmação — saem iguais.
 *
 * O método é do record, não do modelo, porque aqui o horário já é LocalTime solto: o
 * DTO não tem um TimeSlot para delegar.
 */
package com.redekrill.scheduling.application.dto.reference;

import com.redekrill.scheduling.domain.model.TimeSlot;

import java.time.LocalTime;

/**
 * Projeção de leitura de TimeSlot, com formatação de horário.
 */
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
