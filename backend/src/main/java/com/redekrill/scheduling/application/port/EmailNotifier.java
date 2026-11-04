/*
 * Contrato do envio da confirmação de agendamento.
 *
 * Este é o único port que recebe o agregado inteiro em vez de identificadores. A
 * razão é prática: o HTML da confirmação é montado com dez valores lidos do
 * agendamento mais a lista de avisos de capacidade, e transformar isso em parâmetros
 * de método daria uma assinatura com onze argumentos apenas para esconder o
 * Appointment.
 *
 * Quem implementa é infrastructure.email.SmtpEmailNotifier, com Thymeleaf escrevendo
 * o corpo, chamado por EmailOutboxProcessor na varredura em segundo plano. Durante a
 * requisição o caso de uso nem fala com este port — ele grava a intenção na fila, e é
 * o processador quem enfrenta o SMTP.
 */
package com.redekrill.scheduling.application.port;

import com.redekrill.scheduling.domain.model.Appointment;

import java.util.List;

/**
 * Envio da confirmação, com os avisos que devem aparecer na mensagem.
 */
public interface EmailNotifier {

    void sendAppointmentConfirmation(Appointment appointment, List<String> warnings);
}
