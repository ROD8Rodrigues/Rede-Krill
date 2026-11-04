/*
 * Pedido de cadastro de um tipo de paletização.
 *
 * capacityMultiplier é Integer e não int porque o campo é opcional na leitura: a
 * criação assume 1 quando ele vem nulo, e o modelo depois manda em quem manda de
 * verdade — PALETIZADO vale 1 e NAO_PALETIZADO vale 2, não importa o que o request
 * trouxe. Só o código livre aceita número, com margem de 1 a 100.
 *
 * actorEmail não vem do formulário: é o e-mail do principal autenticado, injetado
 * pela apresentação, e serve para registrar a auditoria com um ator real.
 */
package com.redekrill.scheduling.application.dto.reference;

/**
 * Dados de entrada para criar um tipo de paletização.
 */
public record CreatePalletizationTypeCommand(String name, Integer capacityMultiplier,
                                             String actorEmail) {
}
