/*
 * Edição de faixa horária, com o mesmo par de horários da criação.
 *
 * Quem altera uma faixa já preenchida é avisado pelo caso de uso, não por este record: a
 * consequência está nos agendamentos vinculados, e não há como saber isso olhando para o
 * corpo da request.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateTimeSlotRequest(
        @NotNull(message = "Horário inicial é obrigatório.") LocalTime startTime,
        @NotNull(message = "Horário final é obrigatório.") LocalTime endTime
) {
}
