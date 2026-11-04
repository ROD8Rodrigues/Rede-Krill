/*
 * A ocupação de um dia: total, limite e a abertura por faixa.
 *
 * Vem com o percentual já calculado (dailyOccupancyPercentage) porque a regra de quanto é
 * "quase cheio" pertence à aplicação. Um cliente que refizer a divisão com inteiros
 * mostraria 83% onde o domínio diz 83,3%.
 *
 * A lista de faixas é copiada para imutável no construtor compacto, e cada item traz a
 * própria faixa com label pronto — é o que permite desenhar o quadro do dia sem cruzar
 * com a rota de time-slots.
 */
package com.redekrill.scheduling.presentation.response;

import java.time.LocalDate;
import java.util.List;

public record CapacityOverviewResponse(
        LocalDate date,
        long dailyPallets,
        int dailyLimit,
        double dailyOccupancyPercentage,
        List<SlotCapacityResponse> slots
) {

    public CapacityOverviewResponse {
        slots = List.copyOf(slots);
    }
}
