/*
 * O command da edição de agendamento: mesma lista da criação, mais o id do
 * registro que está sendo alterado.
 *
 * A edição substitui o conteúdo inteiro, não aplica um patch parcial. Isso vem à
 * tona aqui porque cada campo é obrigatório de novo: enviar o formulário com um
 * campo vazio não mantém o valor antigo, ele falha na validação.
 *
 * Não há campo de status. Cancelar é outra operação, com autor e data próprios,
 * e deixar que um comando de edição mexesse no status permitiria reativar um
 * agendamento cancelado sem auditoria de cancelamento.
 */
package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;

/**
 * Parâmetros da alteração de agendamento, incluindo o id do registro visado.
 */
public record UpdateAppointmentCommand(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        Long timeSlotId,
        Long truckTypeId,
        Long palletizationTypeId,
        int palletQuantity,
        String notes,
        String actorEmail
) {
}
