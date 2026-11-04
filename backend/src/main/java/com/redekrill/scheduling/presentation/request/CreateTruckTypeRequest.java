/*
 * Nome obrigatório e descrição opcional do tipo de caminhão.
 *
 * A descrição é o único campo que pode faltar; o nome não, porque é o que aparece na
 * lista do formulário e o que a unicidade protege (índice sobre nome em caixa baixa). Os
 * 255 caracteres da descrição são o tamanho da coluna — passar disso é recusada aqui, não
 * no banco.
 *
 * Capacidade de carga não é campo deste tipo: o que limita pallets é a política de
 * capacidade do dia e da faixa, não o rótulo do caminhão.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTruckTypeRequest(
        @NotBlank(message = "Nome do tipo de caminhão é obrigatório.")
        @Size(max = 80, message = "Nome do tipo de caminhão deve ter no máximo 80 caracteres.")
        String name,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String description
) {
}
