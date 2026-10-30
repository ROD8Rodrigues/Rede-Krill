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
        if (capacityMultiplier < 1) {
            throw new DomainException("Multiplicador de capacidade deve ser maior que zero.");
        }
    }
}
