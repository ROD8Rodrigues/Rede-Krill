/*
 * Contrato da fila de e-mails que vive em tabela.
 *
 * Quatro métodos, e nenhum deles apaga. A linha fica depois de enviada, falhada
 * ou cancelada — ela é o registro do que aconteceu com o aviso, e perder isso é
 * ficar sem resposta para "por que o fornecedor não recebeu a confirmação?".
 *
 * findDue é o único método que entende de tempo: recebe o instante de agora e o
 * tamanho do lote, e devolve só o que já passou do portão de espera. O limit existe
 * para uma varredura não tentar despejar a tabela inteira no SMTP de uma vez, e a
 * ordem de entrega é a de criação, para o aviso mais antigo não ficar para trás
 * enquanto chegam novidades.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Entrada e saída da fila de confirmações: gravar, achar uma, pegar o lote da vez
 * e listar as pendentes de um agendamento.
 *
 * O que esta interface faz: descreve o que o processador em segundo plano e o
 * serviço de enqueue precisam.
 *
 * O que ela NÃO faz: excluir linha nem marcar estado. O estado muda por
 * EmailOutboxMessage.markSent, markFailed e markCanceled, sempre com save.
 */
public interface EmailOutboxRepository {

    EmailOutboxMessage save(EmailOutboxMessage message);

    Optional<EmailOutboxMessage> findById(Long id);

    /**
     * Lote de mensagens prontas para uma tentativa agora.
     *
     * @param now   instante comparado com a próxima tentativa de cada linha.
     * @param limit máximo de mensagens do lote.
     * @return as pendentes liberadas, da mais antiga para a mais nova.
     */
    List<EmailOutboxMessage> findDue(Instant now, int limit);

    /**
     * Pendentes de um agendamento, usadas no cancelamento para suprimir o aviso
     * que ainda não saiu.
     */
    List<EmailOutboxMessage> findPendingByAppointmentId(Long appointmentId);
}
