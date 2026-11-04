/*
 * A fila de e-mails na tabela, em vez de um disparo dentro da requisição.
 *
 * O agendamento existe a partir do momento em que o insert é confirmado. Se o
 * envio do SMTP estivesse no mesmo caminho, um servidor de e-mail fora do ar
 * faria a operação inteira falhar — ou, no melhor caso, deixaria o fornecedor sem
 * saber de um agendamento que está valendo. Então o que a transação grava é esta
 * linha, em PENDING, e um processador em segundo plano cuida de entregar.
 *
 * Consequência prática: o estado deste registro é a única fonte do que aconteceu
 * com o aviso. Quem quiser saber se o fornecedor foi notificado consulta esta
 * tabela; não existe outra fila nem outro cache.
 *
 * warnings é o texto já joinado por quebra de linha, e não uma lista, porque a
 * coluna guarda o que o e-mail vai imprimir. A vazia é representada por string
 * vazia, não por null — o processador testa isBlank() antes de dividir, e null
 * quebraria esse teste.
 */
package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Mensagem de confirmação esperando entrega, com contagem de tentativas e a
 * próxima data permitida.
 *
 * O que esta classe faz: transiciona entre os quatro estados e controla a
 * contagem de tentativas.
 *
 * O que ela NÃO faz: enviar nada nem decidir o tempo de espera. O intervalo de
 * reenvio é calculado pelo processador, que chama markFailed informando quando a
 * próxima tentativa pode acontecer e qual é o teto de tentativas.
 */
public record EmailOutboxMessage(
        Long id,
        // Qual agendamento originou o aviso; a mensagem existe por causa dele.
        Long appointmentId,
        String recipient,
        // Avisos de capacidade, um por linha. Vazio quando o agendamento coube nos limites.
        String warnings,
        EmailOutboxStatus status,
        // Tentativas já queimadas: cresce no sucesso e na falha; cancelamento não conta.
        int attempts,
        // Portal de tempo: o processador só pega a mensagem depois deste instante.
        Instant nextAttemptAt,
        // Último erro recebido, cortado em mil caracteres, só para leitura humana.
        String lastError,
        Instant createdAt,
        // Preenchida apenas em SENT; uma mensagem cancelada permanece com null aqui.
        Instant sentAt
) {

    public EmailOutboxMessage {
        Objects.requireNonNull(appointmentId, "Agendamento da mensagem é obrigatório.");
        recipient = DomainValidation.requireEmail(recipient, "Destinatário é obrigatório.");
        warnings = warnings == null ? "" : warnings;
        Objects.requireNonNull(status, "Status da mensagem é obrigatório.");
        Objects.requireNonNull(nextAttemptAt, "Próxima tentativa é obrigatória.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        if (attempts < 0) {
            throw new com.redekrill.scheduling.domain.exception.DomainException(
                    "Tentativas não podem ser negativas.");
        }
    }

    /**
     * Entrada recém-gravada junto do agendamento: PENDING, zero tentativas e
     * próxima tentativa liberada já no instante da criação.
     *
     * @param appointmentId agendamento que gerou o aviso.
     * @param recipient     e-mail do fornecedor.
     * @param warnings      avisos de capacidade já formatados, separados por quebra de linha.
     * @param now           instante da criação.
     * @return a mensagem pronta para entrar na fila.
     */
    public static EmailOutboxMessage create(Long appointmentId, String recipient,
                                            String warnings, Instant now) {
        return new EmailOutboxMessage(null, appointmentId, recipient, warnings,
                EmailOutboxStatus.PENDING, 0, now, null, now, null);
    }

    /**
     * Copia o valor recebendo o id gerado pelo banco.
     *
     * @param newId identificador devolvido pela persistência.
     * @return a mesma mensagem, com id preenchido.
     */
    public EmailOutboxMessage withId(Long newId) {
        return new EmailOutboxMessage(newId, appointmentId, recipient, warnings, status,
                attempts, nextAttemptAt, lastError, createdAt, sentAt);
    }

    /**
     * Entrega confirmada. Além do estado, a data de envio é o que marca a fila:
     * SENT tem sentAt preenchido, e é por essa coluna que a leitura humana
     * distingue "enviado" de "pendente há três dias".
     *
     * @param now instante em que o servidor de e-mail aceitou a mensagem.
     * @return a mensagem concluída, com a contagem de tentativas somada.
     */
    public EmailOutboxMessage markSent(Instant now) {
        return new EmailOutboxMessage(id, appointmentId, recipient, warnings,
                EmailOutboxStatus.SENT, attempts + 1, now, null, createdAt, now);
    }

    /**
     * Cancelamento do envio porque o agendamento deixou de existir para o fornecedor.
     * Estado terminal: a mensagem fica no histórico, mas sai da fila de trabalho.
     *
     * @param now instante do cancelamento, gravado como data da última movimentação.
     * @return a mensagem em CANCELED, sem sentAt e sem perder o erro anterior.
     */
    public EmailOutboxMessage markCanceled(Instant now) {
        return new EmailOutboxMessage(id, appointmentId, recipient, warnings,
                EmailOutboxStatus.CANCELED, attempts, now, lastError, createdAt, null);
    }

    /**
     * Falha de entrega, decidindo se vale insistir ou se acabou.
     *
     * A fronteira é esta: enquanto a próxima tentativa não ultrapassar o teto, a
     * mensagem continua PENDING e ganha um novo portão de tempo. Ao atingir o teto
     * de tentativas, vira FAILED e para — sem apagar o último erro, que é o que o
     * operador vai ler para descobrir o motivo.
     *
     * @param now           instante da falha.
     * @param error         mensagem do exceção recebida; null é aceito.
     * @param maxAttempts   teto vindo da configuração, comparado com a contagem já somada.
     * @param nextAttemptAt quando o processador poderá tentar de novo; irrelevante em FAILED.
     * @return a mensagem em PENDING para insistir, ou em FAILED para parar.
     */
    public EmailOutboxMessage markFailed(Instant now, String error, int maxAttempts,
                                         Instant nextAttemptAt) {
        EmailOutboxStatus newStatus = attempts + 1 >= maxAttempts
                ? EmailOutboxStatus.FAILED : EmailOutboxStatus.PENDING;
        // O corte existe para a coluna não estourar com stack trace de driver de SMTP.
        String truncatedError = error == null ? null
                : error.substring(0, Math.min(error.length(), 1000));
        return new EmailOutboxMessage(id, appointmentId, recipient, warnings,
                newStatus, attempts + 1, nextAttemptAt, truncatedError, createdAt, null);
    }
}
