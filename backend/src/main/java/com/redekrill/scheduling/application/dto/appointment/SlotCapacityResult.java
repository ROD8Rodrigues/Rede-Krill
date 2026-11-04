/*
 * Ocupação de uma única faixa dentro do panorama do dia.
 *
 * É o item da lista de CapacityOverviewResult. Traz a faixa junto do número para
 * que a tela não precise casar posição de lista com cadastro: se o ADMIN cria uma
 * faixa nova entre duas requisições, a linha continua dizendo de quem é aquele
 * total.
 */
package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;

/**
 * Par faixa + pallets considerados + limite + percentual.
 */
public record SlotCapacityResult(
        TimeSlotResult timeSlot,
        long pallets,
        int limit,
        double occupancyPercentage
) {
}
