/*
 * Pedido de alteração de uma faixa horária.
 *
 * Id mais os dois horários — o mesmo par da criação. Não há campo para "meia faixa"
 * nem para dia da semana: uma faixa é sempre um intervalo de um dia, e a duração de
 * duas horas continua sendo regra do modelo.
 *
 * Quem edita um horário precisa saber que o conflito é verificado de novo, agora
 * excluindo a própria faixa. Isso acontece no caso de uso, não aqui.
 */
package com.redekrill.scheduling.application.dto.reference;

import java.time.LocalTime;

/**
 * Identificador e novo intervalo de uma faixa horária.
 */
public record UpdateTimeSlotCommand(Long id, LocalTime startTime, LocalTime endTime,
                                    String actorEmail) {
}
