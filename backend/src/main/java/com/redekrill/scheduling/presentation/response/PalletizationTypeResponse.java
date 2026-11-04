/*
 * Tipo de paletização, incluindo o código estável.
 *
 * code (o enum PalletizationCode) e name aparecem juntos porque o primeiro é o que
 * identifica a opção entre versões — nome é rótulo editável por ADMIN. Um cliente que
 * precisar tomar decisão por tipo deve ler code; quem mostra na tela mostra name.
 *
 * capacityMultiplier vai na resposta porque a tela de agendamento precisa explicar por
 * que X pallets declarados ocupam Y pallets de faixa.
 */
package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.PalletizationCode;

public record PalletizationTypeResponse(
        Long id,
        PalletizationCode code,
        String name,
        int capacityMultiplier
) {
}
