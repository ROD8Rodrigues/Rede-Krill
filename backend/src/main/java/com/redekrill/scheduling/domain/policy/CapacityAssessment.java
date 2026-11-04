/*
 * O resultado de uma conferência de capacidade, junto com os números que o
 * produziram.
 *
 * Este registro não é um booleano "coube" porque a política de capacidade deste
 * sistema é avisar, não barrar. Quem recebe o resultado precisa, então, de três
 * coisas: os totais que foram comparados, os limites usados na comparação e a
 * frase que vai aparecer para o usuário. Devolver só a lista de avisos deixaria a
 * tela de ocupação recalculando tudo por conta própria.
 *
 * Os limites vêm junto do resultado de propósito: a barra de percentagem da tela
 * precisa saber contra qual número ela está desenhando. Se o teto de pallets mudar
 * de lugar, quem calcula e quem exibe continuam concordando porque ambos leram o
 * mesmo objeto.
 */
package com.redekrill.scheduling.domain.policy;

import java.util.List;

/**
 * Total comparado, limite correspondente e avisos produzidos pela
 * {@link CapacityPolicy}.
 *
 * O que esta classe faz: congela a lista de avisos e converte os totais em
 * percentual de ocupação.
 *
 * O que ela NÃO faz: decidir bloqueio. Não há método "permitido" aqui, e não deve
 * aparecer um: a regra do sistema é registrar o agendamento e alertar.
 */
public record CapacityAssessment(
        long dailyPallets,
        long slotPallets,
        int dailyLimit,
        int slotLimit,
        List<String> warnings
) {

    public CapacityAssessment {
        // Lista imutável: quem recebe o resultado não deve conseguir emendar os avisos.
        warnings = List.copyOf(warnings);
    }

    /**
     * Conveniência para quem só quer saber se precisa mostrar alguma coisa.
     *
     * @return true quando algum dos dois limites foi ultrapassado.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Ocupação do dia, em percentual, para a barra da tela de capacidade.
     *
     * @return de zero a mais de cem, com uma casa decimal; pode passar de cem
     *         porque a regra avisa em vez de cortar.
     */
    public double dailyOccupancyPercentage() {
        return CapacityPolicy.occupancyPercentage(dailyPallets, dailyLimit);
    }

    /**
     * Ocupação da faixa horária, calculada sobre a quantidade considerada — que é
     * a quantidade declarada multiplicada pelo tipo de paletização.
     *
     * @return de zero a mais de cem, com uma casa decimal.
     */
    public double slotOccupancyPercentage() {
        return CapacityPolicy.occupancyPercentage(slotPallets, slotLimit);
    }
}
