/*
 * Resposta dos comandos de agendamento: o registro, o que a capacidade disse e o
 * destino do aviso ao fornecedor.
 *
 * As duas flags finais existem porque o e-mail não é enviado durante a requisição.
 * emailQueued diz que a intenção foi gravada na fila; emailSent diz que o SMTP
 * aceitou a mensagem. No momento do comando a segunda é sempre falsa — o envio é
 * do processador, alguns segundos depois.
 *
 * A primeira depende da operação: criação enfileira confirmação (true), edição não
 * enfileira nada (false). Cancelar não devolve este tipo; ele apenas suprime as
 * confirmações que ainda não saíram.
 *
 * warnings é copiado para lista imutável: a resposta não pode ser emendada por
 * quem a recebe.
 */
package com.redekrill.scheduling.application.dto.appointment;

import java.util.List;

/**
 * Resultado de criar, alterar ou cancelar um agendamento.
 */
public record AppointmentCommandResult(
        AppointmentResult appointment,
        List<String> warnings,
        CapacityResult capacity,
        boolean emailQueued,
        boolean emailSent
) {

    public AppointmentCommandResult {
        warnings = List.copyOf(warnings);
    }
}
