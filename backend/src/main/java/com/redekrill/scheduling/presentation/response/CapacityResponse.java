/*
 * Ocupação no momento da operação, em duas escalas.
 *
 * Os dois números não contam a mesma coisa, e é preciso saber isso para não somar um no
 * outro: dailyPallets é a soma dos pallets declarados no dia, enquanto slotPallets é a
 * soma dos pallets efetivos da faixa — estes já ponderados pelo multiplicador de
 * paletização. Cada um compete com o seu limite (480 e 120).
 *
 * Os dois limites vêm lado a lado porque o aviso de capacidade tem dois motivos
 * diferentes — "o dia estourou" e "essa faixa estourou" — e devolver só o percentual
 * deixaria a mensagem sem número para citar.
 *
 * É o retrato depois do agendamento criado ou editado — por isso mora na resposta de
 * comando e não em rota própria. Para consultar um dia sem mexer em nada, existe o
 * overview de capacidade.
 */
package com.redekrill.scheduling.presentation.response;

public record CapacityResponse(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit,
        double dailyOccupancyPercentage,
        double slotOccupancyPercentage
) {
}
