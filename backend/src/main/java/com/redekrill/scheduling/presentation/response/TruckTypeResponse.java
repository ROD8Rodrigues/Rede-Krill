/*
 * Tipo de caminhão, com as duas datas de cadastro.
 *
 * É a única entidade de referência cujas datas de escrita são devolvidas: paletização e
 * faixa horária saem sem elas. Não há regra nisso além de escolha de contrato — e é
 * exatamente por isso que a decisão fica registrada aqui, para quem comparar as três
 * respostas e desconfiar de um esquecimento.
 *
 * Sem campos de capacidade: tipo de caminhão é rótulo e alvo de vínculo, não mede pallet.
 */
package com.redekrill.scheduling.presentation.response;

import java.time.Instant;

public record TruckTypeResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
