/*
 * O que o caso de uso de criação precisa saber, em uma única entrega.
 *
 * O command atravessa a borda da apresentação já com o autor dentro
 * (actorEmail). Isso é o que permite ao caso de uso funcionar sem HttpRequest:
 * quem monta o comando é o controller, que acabou de ler a autenticação, e o caso
 * de uso recebe um fato consumado sobre quem está pedindo.
 *
 * Os três cadastros entram por id, e não por objeto, porque o cliente escolhe
 * entre as opções listadas pela API. Cabe ao caso de uso conferir se aquele id
 * ainda existe — é o que separa um formulário preenchido ontem de um cadastro
 * excluído hoje.
 */
package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;

/**
 * Parâmetros de entrada da criação de agendamento. Sem validação aqui: o formato
 * é conferido pelo caso de uso e pelo construtor de Appointment.
 */
public record CreateAppointmentCommand(
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
