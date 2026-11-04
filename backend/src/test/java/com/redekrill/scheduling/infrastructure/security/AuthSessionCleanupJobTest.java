package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthSessionCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");

    @Test
    void purgesSessionsOlderThanTheRetentionWindow() {
        AuthSessionRepository repository = mock(AuthSessionRepository.class);
        when(repository.deleteExpiredBefore(any())).thenReturn(3);
        AuthSessionCleanupJob job = new AuthSessionCleanupJob(repository,
                properties(Duration.ofDays(7)), Clock.fixed(NOW, ZoneOffset.UTC));

        job.purgeExpiredSessions();

        verify(repository).deleteExpiredBefore(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void usesTheWholeCutoffWhenRetentionIsZero() {
        AuthSessionRepository repository = mock(AuthSessionRepository.class);
        when(repository.deleteExpiredBefore(any())).thenReturn(0);
        AuthSessionCleanupJob job = new AuthSessionCleanupJob(repository,
                properties(Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));

        job.purgeExpiredSessions();

        verify(repository).deleteExpiredBefore(NOW);
    }

    @Test
    void rejectsNegativeRetention() {
        assertThatThrownBy(() -> properties(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("session-expired-retention");
    }

    @Test
    void doesNotRunWhenCleanupIsDisabled() {
        AuthSessionRepository repository = mock(AuthSessionRepository.class);

        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                        "app.security.session-cleanup-enabled=false",
                        "app.security.login-max-attempts-per-account=5",
                        "app.security.login-max-attempts-per-ip=20",
                        "app.security.login-window=15m",
                        "app.security.login-lockout=5m",
                        "app.security.session-expired-retention=7d")
                .withBean(AuthSessionRepository.class, () -> repository)
                .withBean(Clock.class, () -> Clock.fixed(NOW, ZoneOffset.UTC))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuthSessionCleanupJob.class);
                    verifyNoInteractions(repository);
                });
    }

    @Test
    void registersTheJobByDefault() {
        AuthSessionRepository repository = mock(AuthSessionRepository.class);
        when(repository.deleteExpiredBefore(any())).thenReturn(1);

        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                        "app.security.login-max-attempts-per-account=5",
                        "app.security.login-max-attempts-per-ip=20",
                        "app.security.login-window=15m",
                        "app.security.login-lockout=5m",
                        "app.security.session-expired-retention=7d")
                .withBean(AuthSessionRepository.class, () -> repository)
                .withBean(Clock.class, () -> Clock.fixed(NOW, ZoneOffset.UTC))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthSessionCleanupJob.class);
                    context.getBean(AuthSessionCleanupJob.class).purgeExpiredSessions();
                    verify(repository).deleteExpiredBefore(NOW.minus(Duration.ofDays(7)));
                    verify(repository, never()).deleteExpiredBefore(NOW.plusSeconds(1));
                });
    }

    private static SecurityProperties properties(Duration retention) {
        return new SecurityProperties(false, 5, 20, Duration.ofMinutes(15),
                Duration.ofMinutes(5), retention, 10);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AuthSessionCleanupJob.class)
    static class TestConfiguration {
        @Bean
        SecurityProperties securityProperties() {
            return properties(Duration.ofDays(7));
        }
    }
}
