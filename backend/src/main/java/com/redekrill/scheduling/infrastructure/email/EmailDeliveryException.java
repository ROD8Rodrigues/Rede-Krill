/*
 * Exceção de infra para "o e-mail não saiu".
 *
 * Ela existe para um motivo concreto: o chamador (EmailOutboxProcessor) distingue
 * "falha ao entregar" de "erro do próprio processamento" só pelo tipo, e captura
 * RuntimeException de forma geral ao redor do envio. Envolver o MessagingException ou o
 * MailException aqui dá ao processador um nome para a falha e preserva a causa original
 * para o log, que é o que se lê quando a mensagem vira FAILED.
 *
 * É runtime, como tudo neste projeto: falha de envio não muda o resultado do
 * agendamento, então não faz sentido contaminar a assinatura dos casos de uso.
 */
package com.redekrill.scheduling.infrastructure.email;

/**
 * Sinaliza que a entrega pelo SMTP falhou, com a causa técnica embaixo. Não carrega
 * contagem de tentativas — quem decide reter ou desistir é a mensagem do outbox.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
