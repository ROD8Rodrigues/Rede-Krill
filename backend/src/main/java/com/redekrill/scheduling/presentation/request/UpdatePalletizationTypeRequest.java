/*
 * Mesmos campos da criação, tipo separado.
 *
 * A duplicação é barata e compra uma coisa concreta: dá para relaxar ou apertar a edição
 * sem tocar no que a criação aceita. Editar tipo de paletização tem efeito diferente —
 * mexe em faixas já ocupadas — e um dia isso pode virar regra só aqui.
 */
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
