/*
 * Pedido de alteração de um tipo de paletização.
 *
 * Mesmos campos da criação mais o id do registro, e uma ausência proposital: código.
 * O PalletizationCode de um tipo criado não muda — é ele que amarra as regras de
 * multiplicador e as linhas de referência do banco. Quem quiser outro código cria um
 * tipo novo.
 *
 * O multiplicador volta como Integer opcional pela mesma razão da criação: para os
 * dois códigos fixos ele é irrelevante, e o caso de uso trata o nulo antes de chegar a
 * validação do modelo.
 */
package com.redekrill.scheduling.application.dto.reference;

/**
 * Identificador e novos valores de um tipo de paletização.
 */
public record UpdatePalletizationTypeCommand(Long id, String name, Integer capacityMultiplier,
                                             String actorEmail) {
}
