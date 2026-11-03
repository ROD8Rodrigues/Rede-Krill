package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePalletizationTypeRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 80, message = "Nome deve ter no máximo 80 caracteres.")
        String name,

        @Min(value = 1, message = "Multiplicador deve ser no mínimo 1.")
        @Max(value = 100, message = "Multiplicador deve ser no máximo 100.")
        Integer capacityMultiplier
) {
}
