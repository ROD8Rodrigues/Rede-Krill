package com.redekrill.scheduling.domain.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Avalia os limites de capacidade de pallets.
 *
 * <p>A política é deliberadamente não bloqueante: quando um limite é excedido,
 * ela produz avisos para o usuário, mas nunca lança exceção nem impede o cadastro.</p>
 *
 * <p>O limite diário usa a quantidade declarada. O limite da faixa usa a
 * quantidade considerada, que recebe o multiplicador do tipo de paletização.</p>
 */
public final class CapacityPolicy {

    public static final int DAILY_LIMIT = 480;
    public static final int TIME_SLOT_LIMIT = 120;

    public CapacityAssessment assess(long dailyPalletsBefore, long slotPalletsBefore,
                                     int newDailyPallets, int newSlotPallets) {
        if (dailyPalletsBefore < 0 || slotPalletsBefore < 0) {
            throw new IllegalArgumentException("Totais de pallets não podem ser negativos.");
        }
        if (newDailyPallets <= 0) {
            throw new IllegalArgumentException("Pallets do agendamento devem ser positivos.");
        }
        if (newSlotPallets <= 0) {
            throw new IllegalArgumentException("Pallets da faixa devem ser positivos.");
        }

        long dailyTotal = dailyPalletsBefore + newDailyPallets;
        long slotTotal = slotPalletsBefore + newSlotPallets;
        List<String> warnings = new ArrayList<>(2);

        if (dailyTotal > DAILY_LIMIT) {
            warnings.add("Limite diário de %d pallets excedido: %d pallets no dia (excedente de %d)."
                    .formatted(DAILY_LIMIT, dailyTotal, dailyTotal - DAILY_LIMIT));
        }
        if (slotTotal > TIME_SLOT_LIMIT) {
            warnings.add("Limite da faixa horária de %d pallets excedido: %d pallets na faixa (excedente de %d)."
                    .formatted(TIME_SLOT_LIMIT, slotTotal, slotTotal - TIME_SLOT_LIMIT));
        }

        return new CapacityAssessment(
                dailyTotal, slotTotal, DAILY_LIMIT, TIME_SLOT_LIMIT, warnings);
    }

    /**
     * Percentual de ocupação arredondado em uma casa decimal.
     */
    public static double occupancyPercentage(long pallets, int limit) {
        if (limit <= 0) {
            return 0.0;
        }
        return Math.round((pallets * 100.0 / limit) * 10.0) / 10.0;
    }
}
