/*
 * Nome e multiplicador de um tipo de paletização novo.
 *
 * O multiplicador é o único número com consequência de domínio: ele pesa os pallets
 * declarados na hora de medir a ocupação da faixa, então um valor alto infla a ocupação
 * real sem mudar nada do que foi carregado. O intervalo 1 a 100 existe para isso — não
 * para proteger coluna, mas para limitar o estrago de um zero-a-mais.
 *
 * Integer (wrapper), não int, e a diferença é visível no comportamento: campo ausente
 * chega nulo, e o caso de uso trata nulo como multiplicador 1. Com primitivo, o mesmo
 * corpo viria com 0 — valor que @Min(1) recusa — e "não informei o multiplicador" e
 * "informei um multiplicador inválido" deixariam de ser situações distintas.
 */
package com.redekrill.scheduling.presentation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePalletizationTypeRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 80, message = "Nome deve ter no máximo 80 caracteres.")
        String name,

        @Min(value = 1, message = "Multiplicador deve ser no mínimo 1.")
        @Max(value = 100, message = "Multiplicador deve ser no máximo 100.")
        Integer capacityMultiplier
) {
}
