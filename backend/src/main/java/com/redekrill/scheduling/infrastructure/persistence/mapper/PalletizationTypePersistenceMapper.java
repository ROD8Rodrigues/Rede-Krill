/*
 * Tipo de paletização entre tabela e modelo: quatro campos, nenhum segredo.
 *
 * code e capacityMultiplier viajam como vieram — a regra de qual multiplicador
 * pertence a qual código é do modelo PalletizationType, e ela corre no construtor
 * dele. Isso significa que um cadastro inconsistente no banco (por exemplo, um
 * OUTRO com multiplicador 400) faz a leitura falhar em vez de passar batido, o que é
 * o comportamento desejado para dados de referência.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;

/**
 * Conversão entre PalletizationTypeJpaEntity e o modelo PalletizationType.
 */
public final class PalletizationTypePersistenceMapper {

    private PalletizationTypePersistenceMapper() {
    }

    public static PalletizationType toDomain(PalletizationTypeJpaEntity entity) {
        return new PalletizationType(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCapacityMultiplier()
        );
    }

    public static PalletizationTypeJpaEntity toEntity(PalletizationType type) {
        PalletizationTypeJpaEntity entity = new PalletizationTypeJpaEntity();
        entity.setId(type.id());
        entity.setCode(type.code());
        entity.setName(type.name());
        entity.setCapacityMultiplier(type.capacityMultiplier());
        return entity;
    }
}
