package com.redekrill.scheduling.domain.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Avalia os limites de capacidade de pallets.
 *
 * <p>A política é deliberadamente não bloqueante: quando um limite é excedido,
 * ela produz avisos para o usuário, mas nunca lança exceção nem impede o cadastro.</p>
 */
public final class CapacityPolicy {

    public static final int DAILY_LIMIT = 480;
    public static final int TIME_SLOT_LIMIT = 120;

    public CapacityAssessment assess(long dailyPalletsBefore, long slotPalletsBefore,
                                     int newEffectivePallets) {
        if (dailyPalletsBefore < 0 || slotPalletsBefore < 0) {
            throw new IllegalArgumentException("Totais de pallets não podem ser negativos.");
        }
        if (newEffectivePallets <= 0) {
            throw new IllegalArgumentException("Pallets efetivos do agendamento devem ser positivos.");
        }

        long dailyTotal = dailyPalletsBefore + newEffectivePallets;
        long slotTotal = slotPalletsBefore + newEffectivePallets;
        List<String> warnings = new ArrayList<>(2);

        if (dailyTotal > DAILY_LIMIT) {
            warnings.add("Limite diário de %d pallets excedido: %d pallets no dia (excedente de %d)."
                    .formatted(DAILY_LIMIT, dailyTotal, dailyTotal - DAILY_LIMIT));
        }
        if (slotTotal > TIME_SLOT_LIMIT) {
            warnings.add("Limite da faixa horária de %d pallets excedido: %d pallets na faixa (excedente de %d)."
                    .formatted(TIME_SLOT_LIMIT, slotTotal, slotTotal - TIME_SLOT_LIMIT));
        }

        return new CapacityAssessment(dailyTotal, slotTotal, DAILY_LIMIT, TIME_SLOT_LIMIT, warnings);
    }
}
