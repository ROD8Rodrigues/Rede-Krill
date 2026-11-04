package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private static final Instant START = Instant.parse("2026-02-01T12:00:00Z");
    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT = Duration.ofMinutes(5);

    private final MutableClock clock = new MutableClock(START);
    private final LoginAttemptService service =
            new LoginAttemptService(clock, 5, 20, WINDOW, LOCKOUT);

    @Test
    void blocksAccountWhenTheWindowThresholdIsReached() {
        failFiveTimes("alvo@redekrill.com", "10.0.0.1");

        assertThatThrownBy(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(error -> assertThat(((TooManyRequestsException) error).retryAfterSeconds())
                        .isEqualTo(LOCKOUT.toSeconds()));
    }

    @Test
    void allowsLoginAgainOnceTheBlockExpires() {
        failFiveTimes("alvo@redekrill.com", "10.0.0.1");
        clock.advance(LOCKOUT.plusSeconds(1));

        assertThatCode(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void escalatesTheBlockProgressively() {
        failFiveTimes("alvo@redekrill.com", "10.0.0.1");
        clock.advance(LOCKOUT.plusSeconds(1));
        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure("alvo@redekrill.com", "10.0.0.1");
        }

        // 10 falhas dentro da mesma janela elevam o bloqueio para o segundo degrau.
        assertThatThrownBy(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(error -> assertThat(((TooManyRequestsException) error).retryAfterSeconds())
                        .isEqualTo(LOCKOUT.multipliedBy(2).toSeconds()));
    }

    @Test
    void forgetsFailuresThatAgedOutOfTheWindow() {
        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure("alvo@redekrill.com", "10.0.0.1");
        }
        clock.advance(WINDOW.plusSeconds(1));
        service.recordFailure("alvo@redekrill.com", "10.0.0.1");

        assertThatCode(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void successfulLoginClearsOnlyTheAccountCounter() {
        for (int attempt = 0; attempt < 4; attempt++) {
            service.recordFailure("alvo@redekrill.com", "10.0.0.1");
        }
        service.recordSuccess("alvo@redekrill.com");

        // a conta zera; o IP continua contando as quatro tentativas anteriores
        assertThatCode(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .doesNotThrowAnyException();
        for (int attempt = 0; attempt < 16; attempt++) {
            service.recordFailure("outro@redekrill.com", "10.0.0.1");
        }
        assertThatThrownBy(() -> service.assertAllowed("nada@redekrill.com", "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void limitsTheIpCounterAcrossDifferentAccounts() {
        LoginAttemptService perIp = new LoginAttemptService(clock, 5, 3, WINDOW, LOCKOUT);
        perIp.recordFailure("a@redekrill.com", "10.0.0.2");
        perIp.recordFailure("b@redekrill.com", "10.0.0.2");
        perIp.recordFailure("c@redekrill.com", "10.0.0.2");

        assertThatThrownBy(() -> perIp.assertAllowed("d@redekrill.com", "10.0.0.2"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void toleratesMissingEmailAndIp() {
        assertThatCode(() -> {
            service.recordFailure(null, null);
            service.recordFailure(" ", " ");
            service.assertAllowed(null, null);
            service.recordSuccess(null);
        }).doesNotThrowAnyException();
    }

    @Test
    void normalizesTheAccountKey() {
        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure(" Alvo@RedeKrill.com ", "10.0.0.1");
        }

        assertThatThrownBy(() -> service.assertAllowed("alvo@redekrill.com", "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void boundsTrackedAccountKeys() {
        for (int key = 0; key < MAX_TRACKED_KEYS + 5; key++) {
            service.recordFailure("conta" + key + "@redekrill.com", "10.0.0.9");
        }

        assertThat(trackedAccounts()).isLessThanOrEqualTo(MAX_TRACKED_KEYS);
    }

    private void failFiveTimes(String email, String ip) {
        for (int attempt = 0; attempt < 5; attempt++) {
            service.recordFailure(email, ip);
        }
    }

    private int trackedAccounts() {
        try {
            Field field = LoginAttemptService.class.getDeclaredField("accountAttempts");
            field.setAccessible(true);
            return ((Map<?, ?>) field.get(service)).size();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        private void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
