package com.redekrill.scheduling.infrastructure.email;

import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
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
    }

    @Scheduled(fixedDelayString = "${app.email.outbox.poll-interval-ms:30000}")
    public void process() {
        Instant now = Instant.now(clock);
        List<EmailOutboxMessage> due = transactionTemplate.execute(status ->
                outboxRepository.findDue(now, properties.batchSize()));
        if (due == null || due.isEmpty()) {
            return;
        }
        for (EmailOutboxMessage message : due) {
            transactionTemplate.executeWithoutResult(status -> processOne(message.id()));
        }
    }

    void processOne(Long messageId) {
        EmailOutboxMessage message = outboxRepository.findById(messageId).orElse(null);
        if (message == null) {
            return;
        }
        Instant now = Instant.now(clock);
        try {
            Appointment appointment = appointmentRepository.findById(message.appointmentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Agendamento da mensagem não encontrado."));
            List<String> warnings = message.warnings().isBlank()
                    ? List.of()
                    : Arrays.asList(message.warnings().split("\n"));
            emailNotifier.sendAppointmentConfirmation(appointment, warnings);
            outboxRepository.save(message.markSent(now));
            sentCounter.increment();
            log.info("E-mail do agendamento {} enviado.", message.appointmentId());
        } catch (RuntimeException exception) {
            long backoff = Math.min(MAX_BACKOFF_SECONDS,
                    properties.initialBackoffSeconds() * (1L << Math.min(message.attempts(), 10)));
            EmailOutboxMessage failed = message.markFailed(
                    now, exception.getMessage(), properties.maxAttempts(), now.plusSeconds(backoff));
            outboxRepository.save(failed);
            if (failed.status() == com.redekrill.scheduling.domain.model.EmailOutboxStatus.FAILED) {
                failedCounter.increment();
                log.error("E-mail do agendamento {} falhou definitivamente após {} tentativas.",
                        message.appointmentId(), failed.attempts(), exception);
            } else {
                retryCounter.increment();
                log.warn("Falha temporária no e-mail do agendamento {}; nova tentativa em {}s.",
                        message.appointmentId(), backoff);
            }
        }
    }
}
