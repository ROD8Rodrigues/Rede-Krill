/*
 * Entrada e saída da fila de confirmações, do lado da aplicação.
 *
 * enqueue é chamado dentro da transação do agendamento: grava a intenção e devolve
 * nada. Nada aqui espera o SMTP, e é isso que faz o cadastro continuar funcionando
 * com o servidor de e-mail fora do ar.
 *
 * cancelPending existe porque o cancelamento precisa desfazer o aviso que ainda não
 * saiu. Busca só as pendentes daquele agendamento, marca cada uma como CANCELED e
 * devolve quantas foram suprimidas — número que o teste confere, e que o caso de uso
 * de cancelamento, hoje, despreza. Enviar depois de cancelado seria avisar o
 * fornecedor de uma hora que não vale mais.
 *
 * Os avisos chegam como List<String> e são gravados unidos por quebra de linha,
 * porque a coluna é texto e o e-mail imprime uma linha por aviso. Lista nula vira
 * texto vazio, não exceção: aviso de capacidade é detalhe, não pré-condição.
 */
package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Grava a intenção de envio na mesma transação do agendamento. O envio efetivo
 * é realizado pelo processador com retry, sem bloquear o cadastro.
 */
public class EmailOutboxService {

    private final EmailOutboxRepository repository;
    private final Clock clock;

    public EmailOutboxService(EmailOutboxRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void enqueue(Appointment appointment, List<String> warnings) {
        String joinedWarnings = warnings == null ? "" : String.join("\n", warnings);
        repository.save(EmailOutboxMessage.create(
                appointment.id(),
                appointment.supplierEmail(),
                joinedWarnings,
                Instant.now(clock)
        ));
    }

    /**
     * Suprime as confirmações ainda pendentes de um agendamento cancelado. A mensagem
     * permanece no histórico com status terminal, então o processador não a reenvia.
     */
    public int cancelPending(Long appointmentId) {
        Instant now = Instant.now(clock);
        List<EmailOutboxMessage> pending = repository.findPendingByAppointmentId(appointmentId);
        pending.forEach(message -> repository.save(message.markCanceled(now)));
        return pending.size();
    }
}
