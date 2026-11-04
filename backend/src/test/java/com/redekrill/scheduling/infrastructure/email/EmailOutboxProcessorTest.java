package com.redekrill.scheduling.infrastructure.email;

import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.model.EmailOutboxStatus;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.infrastructure.config.properties.EmailOutboxProperties;
import com.redekrill.scheduling.support.TestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxProcessorTest {

    @Mock
    private EmailOutboxRepository outboxRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private EmailNotifier emailNotifier;

    private final CountingTransactionManager transactionManager = new CountingTransactionManager();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private EmailOutboxProperties properties;
    private EmailOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new EmailOutboxProperties(true, 5, 30_000L, 60L, 20);
        processor = new EmailOutboxProcessor(
                outboxRepository,
                appointmentRepository,
                emailNotifier,
                properties,
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC),
                transactionManager,
                meterRegistry
        );
    }

    @Test
    void doesNothingWhenNoMessageIsDue() {
        when(outboxRepository.findDue(any(), anyInt())).thenReturn(List.of());

        processor.process();

        verifyNoInteractions(appointmentRepository, emailNotifier);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void sendsAndMarksMessageSentOutsideOfAnyTransaction() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));
        AtomicInteger transactionsOpenWhileSending = new AtomicInteger(-1);
        doAnswer(invocation -> {
            transactionsOpenWhileSending.set(transactionManager.openTransactions());
            return null;
        }).when(emailNotifier).sendAppointmentConfirmation(any(), any());

        processor.process();

        assertThat(transactionsOpenWhileSending.get()).isZero();
        assertThat(captureSaved().status()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(counter("sent")).isEqualTo(1.0);
    }

    @Test
    void splitsStoredWarningsIntoTheListGivenToTheNotifier() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "Aviso um\nAviso dois");
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));

        processor.process();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(emailNotifier).sendAppointmentConfirmation(any(), captor.capture());
        assertThat(captor.getValue()).containsExactly("Aviso um", "Aviso dois");
    }

    @Test
    void suppressesConfirmationOfCanceledAppointment() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.canceledAppointment()));

        processor.process();

        EmailOutboxMessage saved = captureSaved();
        assertThat(saved.status()).isEqualTo(EmailOutboxStatus.CANCELED);
        assertThat(saved.sentAt()).isNull();
        assertThat(counter("canceled")).isEqualTo(1.0);
        verifyNoInteractions(emailNotifier);
    }

    @Test
    void retriesWhenTheNotifierFails() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));
        doThrow(new EmailDeliveryException("SMTP indisponível", new IllegalStateException("conexão recusada")))
                .when(emailNotifier).sendAppointmentConfirmation(any(), any());

        processor.process();

        EmailOutboxMessage saved = captureSaved();
        assertThat(saved.status()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(saved.attempts()).isEqualTo(1);
        assertThat(saved.lastError()).isEqualTo("SMTP indisponível");
        assertThat(saved.nextAttemptAt()).isEqualTo(TestFixtures.NOW.plusSeconds(60));
        assertThat(counter("retry")).isEqualTo(1.0);
    }

    @Test
    void keepsRetryWindowInsideTheConfiguredCeiling() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "").markFailed(
                TestFixtures.NOW, "erro", 50, TestFixtures.NOW);
        EmailOutboxMessage deep = message.markFailed(
                TestFixtures.NOW, "erro", 50, TestFixtures.NOW).markFailed(
                TestFixtures.NOW, "erro", 50, TestFixtures.NOW);
        givenDueMessages(deep);
        givenMessage(deep);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));
        doThrow(new EmailDeliveryException("SMTP indisponível", new IllegalStateException("conexão recusada")))
                .when(emailNotifier).sendAppointmentConfirmation(any(), any());

        processor.process();

        // attempts=3 com backoff inicial de 60s: 60 * 2^3 = 480s, ainda abaixo do teto de 1h.
        assertThat(captureSaved().nextAttemptAt()).isEqualTo(TestFixtures.NOW.plusSeconds(480));
    }

    @Test
    void capsBackoffAtOneHour() {
        EmailOutboxProperties oneSecondBase = new EmailOutboxProperties(true, 5, 30_000L, 3600L, 20);
        EmailOutboxProcessor local = new EmailOutboxProcessor(outboxRepository, appointmentRepository,
                emailNotifier, oneSecondBase, Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC),
                transactionManager, meterRegistry);
        EmailOutboxMessage message = pendingMessage(1L, 10L, "").markFailed(
                TestFixtures.NOW, "erro", 50, TestFixtures.NOW);
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));
        doThrow(new EmailDeliveryException("SMTP indisponível", new IllegalStateException("conexão recusada")))
                .when(emailNotifier).sendAppointmentConfirmation(any(), any());

        local.process();

        assertThat(captureSaved().nextAttemptAt()).isEqualTo(TestFixtures.NOW.plusSeconds(3600));
    }

    @Test
    void marksFailedDefinitelyWhenAttemptsAreExhausted() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "")
                .markFailed(TestFixtures.NOW, "erro", 5, TestFixtures.NOW)
                .markFailed(TestFixtures.NOW, "erro", 5, TestFixtures.NOW)
                .markFailed(TestFixtures.NOW, "erro", 5, TestFixtures.NOW)
                .markFailed(TestFixtures.NOW, "erro", 5, TestFixtures.NOW);
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));
        doThrow(new EmailDeliveryException("SMTP indisponível", new IllegalStateException("conexão recusada")))
                .when(emailNotifier).sendAppointmentConfirmation(any(), any());

        processor.process();

        EmailOutboxMessage saved = captureSaved();
        assertThat(saved.status()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(saved.attempts()).isEqualTo(5);
        assertThat(counter("failed")).isEqualTo(1.0);
    }

    @Test
    void registersFailureWhenTheAppointmentDisappeared() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        givenDueMessages(message);
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.empty());

        processor.process();

        EmailOutboxMessage saved = captureSaved();
        assertThat(saved.lastError()).isEqualTo("Agendamento da mensagem não encontrado.");
        assertThat(saved.status()).isEqualTo(EmailOutboxStatus.PENDING);
        verifyNoInteractions(emailNotifier);
    }

    @Test
    void ignoresMessageThatIsNoLongerPending() {
        EmailOutboxMessage sent = pendingMessage(1L, 10L, "").markSent(TestFixtures.NOW);
        givenDueMessages(sent);
        givenMessage(sent);

        processor.process();

        verify(appointmentRepository, never()).findById(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void ignoresMessageThatWasRemovedBetweenFetchAndProcessing() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        when(outboxRepository.findDue(TestFixtures.NOW, 20)).thenReturn(List.of(message));
        when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

        processor.process();

        verifyNoInteractions(appointmentRepository, emailNotifier);
    }

    @Test
    void continuesTheBatchWhenOneMessageCannotBeRead() {
        EmailOutboxMessage broken = pendingMessage(1L, 10L, "");
        EmailOutboxMessage healthy = pendingMessage(2L, 20L, "");
        when(outboxRepository.findDue(TestFixtures.NOW, 20)).thenReturn(List.of(broken, healthy));
        when(outboxRepository.findById(1L)).thenThrow(new IllegalStateException("linha corrompida"));
        when(outboxRepository.findById(2L)).thenReturn(Optional.of(healthy));
        when(appointmentRepository.findById(20L)).thenReturn(Optional.of(TestFixtures.appointment()));

        processor.process();

        assertThat(captureSaved().id()).isEqualTo(2L);
        assertThat(captureSaved().status()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(counter("sent")).isEqualTo(1.0);
    }

    @Test
    void processesTheMessageDirectlyWhenCalledWithoutTheBatch() {
        EmailOutboxMessage message = pendingMessage(1L, 10L, "");
        givenMessage(message);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(TestFixtures.appointment()));

        processor.processOne(1L);

        assertThat(captureSaved().status()).isEqualTo(EmailOutboxStatus.SENT);
    }

    private void givenDueMessages(EmailOutboxMessage message) {
        when(outboxRepository.findDue(TestFixtures.NOW, 20)).thenReturn(List.of(message));
    }

    private void givenMessage(EmailOutboxMessage message) {
        when(outboxRepository.findById(message.id())).thenReturn(Optional.of(message));
    }

    private EmailOutboxMessage captureSaved() {
        ArgumentCaptor<EmailOutboxMessage> captor = ArgumentCaptor.forClass(EmailOutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        return captor.getValue();
    }

    private double counter(String status) {
        return meterRegistry.get("email_outbox_processed_total").tag("status", status).counter().count();
    }

    private static EmailOutboxMessage pendingMessage(Long id, Long appointmentId, String warnings) {
        return new EmailOutboxMessage(id, appointmentId, "fornecedor@example.com", warnings,
                EmailOutboxStatus.PENDING, 0, TestFixtures.NOW, null, TestFixtures.NOW, null);
    }

    /** Gerente de transações mínimo que permite afirmar que o envio ocorre sem transação aberta. */
    private static final class CountingTransactionManager implements PlatformTransactionManager {

        private int open;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            open++;
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            open--;
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            open--;
        }

        private int openTransactions() {
            return open;
        }
    }
}
