/*
 * Fotografia de ocupação de um dia inteiro.
 *
 * É uma leitura diferente da que CapacityResult atende: ali se mostra o efeito de
 * um lançamento, aqui se mostra o estado de todo o dia, faixa por faixa. A lista de
 * slots vem completa, inclusive das faixas vazias com zero pallets, porque uma
 * tela de ocupação que só lista o que tem carga não responde "sobrou espaço?".
 *
 * dailyPallets é a soma da quantidade declarada dos agendamentos ativos; as faixas
 * somam a quantidade considerada. Os dois números do objeto, portanto, não são a
 * mesma grandeza nem deveriam ser comparados entre si.
 */
package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;
import java.util.List;

/**
 * Ocupação do dia e de cada faixa horária, com os limites usados na comparação.
 */
public record CapacityOverviewResult(
        LocalDate date,
        long dailyPallets,
        int dailyLimit,
        double dailyOccupancyPercentage,
        List<SlotCapacityResult> slots
) {

    public CapacityOverviewResult {
        slots = List.copyOf(slots);
    }
}
