/*
 * Estourou o limite de tentativas — é o lockout de login.
 *
 * Além da mensagem, carrega quantos segundos o cliente deve esperar. Esse número
 * vem do bloqueio progressivo, que cresce a cada estouro, e sai no header
 * Retry-After do 429. Sem ele, o frontend teria que adivinhar quando voltar a
 * tentar — e adivinhar, aqui, só piora o próprio bloqueio.
 */
package com.redekrill.scheduling.domain.exception;

/**
 * Muitas tentativas em pouco tempo. Vira 429, com Retry-After.
 */
public class TooManyRequestsException extends DomainException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
