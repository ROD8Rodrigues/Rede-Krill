/*
 * Pedido de cadastro de uma faixa horária.
 *
 * São dois horários e nada mais: nem rótulo, nem duração, nem prioridade. Tudo que o
 * tipo de faixa significa — início antes do fim, janela de duas horas, conflito com
 * outra faixa do mesmo dia — é regra do modelo TimeSlot e das consultas do
 * repositório, não daqui.
 *
 * A apresentação entrega LocalTime já convertido; o pedido não valida, apenas
 * transporta.
 */
package com.redekrill.scheduling.application.dto.reference;

import java.time.LocalTime;

/**
 * Início e fim da faixa horária que se quer cadastrar.
 */
public record CreateTimeSlotCommand(LocalTime startTime, LocalTime endTime, String actorEmail) {
}
