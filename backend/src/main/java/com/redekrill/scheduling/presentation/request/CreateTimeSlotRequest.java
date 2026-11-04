/*
 * Início e fim de uma faixa horária, como LocalTime do dia.
 *
 * Nada aqui diz que o fim deve ser depois do início, que a faixa deve ter duas horas ou
 * que ela não pode colidir com outra: as três decisões pertencem ao domínio, onde já
 * existem, e há constraint no banco por baixo (ck_time_slot_order e o único sobre
 * start/end). Validar aqui de novo seria criar duas verdades para a mesma regra.
 *
 * O tipo LocalTime (sem data) é o que permite tratar faixa como relógio do dia — a data
 * do agendamento é campo separado.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateTimeSlotRequest(
        @NotNull(message = "Horário inicial é obrigatório.") LocalTime startTime,
        @NotNull(message = "Horário final é obrigatório.") LocalTime endTime
) {
}
