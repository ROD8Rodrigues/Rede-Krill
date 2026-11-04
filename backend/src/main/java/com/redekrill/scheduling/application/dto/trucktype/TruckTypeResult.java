/*
 * Um tipo de caminhão como ele é devolvido.
 *
 * É a única projeção de cadastro referenciado que leva createdAt e updatedAt. Os dois
 * carimbos vêm do modelo, onde a edição substitui o registro já com o instante da
 * mudança. A auditoria conta o que mudou e quem mudou; estas duas colunas contam o
 * quando, e ficam mais perto do dado.
 *
 * description pode chegar nula, conforme o que foi cadastrado.
 */
package com.redekrill.scheduling.application.dto.trucktype;

import com.redekrill.scheduling.domain.model.TruckType;

import java.time.Instant;

/**
 * Projeção de leitura de TruckType, com carimbos de tempo.
 */
public record TruckTypeResult(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static TruckTypeResult from(TruckType truckType) {
        return new TruckTypeResult(
                truckType.id(),
                truckType.name(),
                truckType.description(),
                truckType.createdAt(),
                truckType.updatedAt()
        );
    }
}
