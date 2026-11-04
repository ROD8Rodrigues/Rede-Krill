/*
 * Pedido de alteração de um tipo de caminhão.
 *
 * Id, nome e descrição — a criação com o identificador do registro. Não há campo para
 * desativar um tipo: ele continua ou sai do catálogo, e a saída tem caso de uso próprio
 * porque esbarra na integridade dos agendamentos que já o usaram.
 *
 * actorEmail, como em toda escrita, é o principal autenticado lido da requisição.
 */
package com.redekrill.scheduling.application.dto.trucktype;

/**
 * Identificador e novos textos de um tipo de caminhão.
 */
public record UpdateTruckTypeCommand(
        Long id,
        String name,
        String description,
        String actorEmail
) {
}
