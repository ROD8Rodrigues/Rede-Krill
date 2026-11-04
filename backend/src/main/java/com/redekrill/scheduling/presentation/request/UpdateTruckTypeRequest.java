/*
 * Renomeação ou ajuste de descrição de um tipo de caminhão.
 *
 * Mesma forma da criação e o mesmo motivo para ser um tipo à parte: PUT exige os dois
 * campos conhecidos, e não há estado parcial a representar. Se um dia a edição proibir
 * mexer no nome de um tipo em uso, a regra nasce aqui ou no caso de uso — nunca
 * compartilhando este record com o POST.
 */
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
