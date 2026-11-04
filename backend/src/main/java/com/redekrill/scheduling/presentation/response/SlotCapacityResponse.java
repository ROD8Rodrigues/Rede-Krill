/*
 * Uma faixa horária com quanto ela está ocupada.
 *
 * Item da lista do overview do dia. Três números e uma referência: pallets efetivos
 * somados na faixa, o limite dela e o percentual calculado sobre os dois, mais o objeto
 * da faixa com início, fim e label.
 *
 * O pallets aqui é o efetivo (ponderado pelo multiplicador de paletização), não o
 * declarado — é o número que compete com o limite da faixa.
 */
package com.redekrill.scheduling.presentation.response;

public record SlotCapacityResponse(
        TimeSlotResponse timeSlot,
        long pallets,
        int limit,
        double occupancyPercentage
) {
}
