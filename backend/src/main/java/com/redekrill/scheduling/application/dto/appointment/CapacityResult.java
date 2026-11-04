/*
 * A medição de capacidade em forma de resposta.
 *
 * Vem de CapacityAssessment com os percentuais já calculados, para que o frontend
 * desenhe a barra sem repetir a regra. É por isso que os limites vão junto: quem
 * exibe precisa saber contra qual número o percentual foi medido, e um número
 * órfão na tela perde o sentido quando o limite muda.
 *
 * O percentual pode passar de cem. Não há corte aqui nem no cálculo — ultrapassar o
 * limite monitorado é exatamente o estado que se quer mostrar.
 */
package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.domain.policy.CapacityAssessment;

/**
 * Projeção de CapacityAssessment: totais, limites e ocupação percentual.
 */
public record CapacityResult(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit,
        double dailyOccupancyPercentage,
        double slotOccupancyPercentage
) {

    public static CapacityResult from(CapacityAssessment assessment) {
        return new CapacityResult(
                assessment.dailyPallets(),
                assessment.slotPallets(),
                assessment.dailyLimit(),
                assessment.slotLimit(),
                assessment.dailyOccupancyPercentage(),
                assessment.slotOccupancyPercentage()
        );
    }
}
