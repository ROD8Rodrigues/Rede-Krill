package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTruckTypeRequest(
        @NotBlank(message = "Nome do tipo de caminhão é obrigatório.")
        @Size(max = 80, message = "Nome do tipo de caminhão deve ter no máximo 80 caracteres.")
        String name,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String description
) {
}
