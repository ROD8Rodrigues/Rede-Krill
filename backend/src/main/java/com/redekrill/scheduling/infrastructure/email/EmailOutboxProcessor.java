/*
 * O entregador da fila de e-mails.
 *
 * O desenho que importa: cada fase tem transação própria e curta — ler o lote, ler a
 * mensagem, ler o agendamento, gravar o resultado — e o SMTP acontece entre elas, com o
 * banco livre. Manter transação aberta durante uma chamada de rede é o jeito mais rápido
 * de esgotar o pool quando o servidor de mail demora (ver ADR-003).
 *
 * Antes de enviar, duas checages reescrevem o destino da mensagem: agendamento que
 * sumiu da tabela vira falha, e agendamento cancelado vira CANCELED — ninguém recebe
 * confirmação de algo que já não existe. A reescrita acontece sem tocar no SMTP.
 *
 * A falha de uma mensagem não derruba o lote: process() captura RuntimeException por id
 * e segue para a próxima, porque um endereço inválido na fila não é motivo para os outros
 * vinte fornecedores ficarem sem aviso.
 *
 * O reteste dobra o espaçamento a cada tentativa (inicial * 2^tentativas, com teto de
 * uma hora e deslocamento limitado a 10 para a multiplicação não estourar long) e
 * marca FAILED quando as tentativas acabam. Quatro contadores Micrometer, todos na
 * mesma série email_outbox_processed_total diferenciada por status, são o que se observa
 * em produção para saber se a fila anda.
 */
package com.redekrill.scheduling.infrastructure.email;

import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.model.EmailOutboxStatus;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.infrastructure.config.properties.EmailOutboxProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entrega as mensagens do outbox. O envio SMTP acontece entre duas transações curtas
 * (leitura da mensagem e gravação do resultado) e nunca dentro de uma transação aberta
 * contra o banco — ver ADR-003. A falha de uma mensagem não interrompe o restante do lote.
 */
@Component
@ConditionalOnProperty(name = "app.email.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class EmailOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxProcessor.class);
    private static final long MAX_BACKOFF_SECONDS = 3600;

    private final EmailOutboxRepository outboxRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailNotifier emailNotifier;
    private final EmailOutboxProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter retryCounter;
    private final Counter canceledCounter;

    public EmailOutboxProcessor(EmailOutboxRepository outboxRepository,
                                AppointmentRepository appointmentRepository,
                                EmailNotifier emailNotifier,
                                EmailOutboxProperties properties,
                                Clock clock,
                                PlatformTransactionManager transactionManager,
                                MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.appointmentRepository = appointmentRepository;
        this.emailNotifier = emailNotifier;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.sentCounter = meterRegistry.counter("email_outbox_processed_total", "status", "sent");
        this.failedCounter = meterRegistry.counter("email_outbox_processed_total", "status", "failed");
        this.retryCounter = meterRegistry.counter("email_outbox_processed_total", "status", "retry");
        this.canceledCounter = meterRegistry.counter("email_outbox_processed_total", "status", "canceled");
    }

    @Scheduled(fixedDelayString = "${app.email.outbox.poll-interval-ms:30000}")
    public void process() {
        Instant now = Instant.now(clock);
        List<Long> dueIds = inTransaction(() -> outboxRepository.findDue(now, properties.batchSize())
                .stream()
                .map(EmailOutboxMessage::id)
                .toList());
        if (dueIds == null) {
            return;
        }
        for (Long messageId : dueIds) {
            try {
                processOne(messageId);
            } catch (RuntimeException exception) {
                log.error("Mensagem {} do outbox não pôde ser processada; o restante do lote segue.",
                        messageId, exception);
            }
        }
    }

    void processOne(Long messageId) {
        EmailOutboxMessage message = inTransaction(() -> outboxRepository.findById(messageId).orElse(null));
        if (message == null || message.status() != EmailOutboxStatus.PENDING) {
            return;
        }
        Instant now = Instant.now(clock);
        Appointment appointment = inTransaction(
                () -> appointmentRepository.findById(message.appointmentId()).orElse(null));
        if (appointment == null) {
            registerFailure(message, now, "Agendamento da mensagem não encontrado.", null);
            return;
        }
        if (appointment.status() == AppointmentStatus.CANCELED) {
            inTransaction(() -> outboxRepository.save(message.markCanceled(now)));
            canceledCounter.increment();
            log.info("Confirmação do agendamento {} cancelada junto do agendamento.", message.appointmentId());
            return;
        }
        List<String> warnings = message.warnings().isBlank()
                ? List.of()
                : Arrays.asList(message.warnings().split("\n"));
        try {
            emailNotifier.sendAppointmentConfirmation(appointment, warnings);
        } catch (RuntimeException exception) {
            registerFailure(message, now, exception.getMessage(), exception);
            return;
        }
        inTransaction(() -> outboxRepository.save(message.markSent(now)));
        sentCounter.increment();
        log.info("E-mail do agendamento {} enviado.", message.appointmentId());
    }

    private void registerFailure(EmailOutboxMessage message, Instant now, String error, RuntimeException cause) {
        long backoff = Math.min(MAX_BACKOFF_SECONDS,
                properties.initialBackoffSeconds() * (1L << Math.min(message.attempts(), 10)));
        EmailOutboxMessage failed = message.markFailed(
                now, error, properties.maxAttempts(), now.plusSeconds(backoff));
        inTransaction(() -> outboxRepository.save(failed));
        if (failed.status() == EmailOutboxStatus.FAILED) {
            failedCounter.increment();
            log.error("E-mail do agendamento {} falhou definitivamente após {} tentativas.",
                    message.appointmentId(), failed.attempts(), cause);
        } else {
            retryCounter.increment();
            log.warn("Falha temporária no e-mail do agendamento {}; nova tentativa em {}s: {}",
                    message.appointmentId(), backoff, error);
        }
    }

    private <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
