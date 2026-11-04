package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.model.EmailOutboxStatus;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {

    @Mock
    private EmailOutboxRepository repository;

    private EmailOutboxService service;

    @BeforeEach
    void setUp() {
        service = new EmailOutboxService(repository, Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC));
    }

    @Test
    void enqueuesPendingMessageWithJoinedWarnings() {
        service.enqueue(TestFixtures.appointment(), List.of("Aviso 1", "Aviso 2"));

        EmailOutboxMessage saved = captureSaved();
        assertThat(saved.status()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(saved.attempts()).isZero();
        assertThat(saved.appointmentId()).isEqualTo(TestFixtures.appointment().id());
        assertThat(saved.recipient()).isEqualTo(TestFixtures.appointment().supplierEmail());
        assertThat(saved.warnings()).isEqualTo("Aviso 1\nAviso 2");
        assertThat(saved.nextAttemptAt()).isEqualTo(TestFixtures.NOW);
    }

    @Test
    void enqueuesEmptyWarningsWhenNoneProvided() {
        service.enqueue(TestFixtures.appointment(), null);

        assertThat(captureSaved().warnings()).isEmpty();
    }

    @Test
    void cancelsEveryPendingMessageOfTheAppointment() {
        when(repository.findPendingByAppointmentId(7L)).thenReturn(List.of(
                message(11L), message(12L)));

        int canceled = service.cancelPending(7L);

        assertThat(canceled).isEqualTo(2);
        ArgumentCaptor<EmailOutboxMessage> captor = ArgumentCaptor.forClass(EmailOutboxMessage.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(EmailOutboxMessage::status)
                .containsOnly(EmailOutboxStatus.CANCELED);
        assertThat(captor.getAllValues())
                .extracting(EmailOutboxMessage::id)
                .containsExactly(11L, 12L);
    }

    @Test
    void doesNothingWhenTheAppointmentHasNoPendingMessage() {
        when(repository.findPendingByAppointmentId(7L)).thenReturn(List.of());

        assertThat(service.cancelPending(7L)).isZero();
        verify(repository, never()).save(any());
    }

    private EmailOutboxMessage captureSaved() {
        ArgumentCaptor<EmailOutboxMessage> captor = ArgumentCaptor.forClass(EmailOutboxMessage.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private EmailOutboxMessage message(Long id) {
        return EmailOutboxMessage.create(7L, "fornecedor@example.com", "", TestFixtures.NOW).withId(id);
    }
}
