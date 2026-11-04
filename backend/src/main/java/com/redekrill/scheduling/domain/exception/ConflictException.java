/*
 * O estado do mundo não permite a operação, mesmo com os dados bem formados.
 *
 * Exemplos reais: criar um agendamento com um orderId que já existe, alterar o
 * que já foi cancelado, ou gravar por cima de uma linha que mudou desde a leitura.
 * Vira 409 — o cliente precisa reler antes de tentar de novo.
 */
package com.redekrill.scheduling.domain.exception;

/**
 * Conflito de estado ou de unicidade. Vira 409 na resposta HTTP.
 */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
