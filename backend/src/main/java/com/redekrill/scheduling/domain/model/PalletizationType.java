package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.util.Objects;

public record PalletizationType(
        Long id,
        PalletizationCode code,
        String name,
        int capacityMultiplier
) {

    public PalletizationType {
        Objects.requireNonNull(code, "Código do tipo de paletização é obrigatório.");
        name = DomainValidation.requireText(name, "Nome do tipo de paletização é obrigatório.");
        if (code == PalletizationCode.PALETIZADO && capacityMultiplier != 1) {
            throw new DomainException("O tipo Paletizado deve manter multiplicador 1.");
        }
        if (code == PalletizationCode.NAO_PALETIZADO && capacityMultiplier != 2) {
            throw new DomainException("O tipo Não Paletizado deve manter multiplicador 2.");
        }
        if (capacityMultiplier < 1 || capacityMultiplier > 100) {
            throw new DomainException("Multiplicador de capacidade deve estar entre 1 e 100.");
        }
    }

    public boolean mandatory() {
        return code != PalletizationCode.OUTRO;
    }
}
