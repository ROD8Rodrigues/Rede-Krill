package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.RefreshTokenGenerator;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    private static final String RAW_TOKEN = "token-de-refresh-em-claro";
    private static final String TOKEN_HASH = "hash-do-token";
    private static final Duration REFRESH_EXPIRATION = Duration.ofDays(7);

    @Mock
    private AuthSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenIssuer tokenIssuer;
    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;
    @Mock
    private AuditService auditService;

    private AuthSessionService service;

    @BeforeEach
    void setUp() {
        service = new AuthSessionService(
                sessionRepository,
                userRepository,
                tokenIssuer,
                refreshTokenGenerator,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC),
                REFRESH_EXPIRATION,
                auditService
        );
    }

    @Test
    void startsSessionAndIssuesTokenBoundToIt() {
        givenNewSessionSaved(10L);
        when(tokenIssuer.issue(TestFixtures.user(), 10L))
                .thenReturn(new IssuedToken("jwt", 900L));

        SessionTokens tokens = service.start(TestFixtures.user(), "10.0.0.1", "curl");

        assertThat(tokens.refreshToken()).isEqualTo("novo-token-gerado");
        assertThat(tokens.sessionId()).isEqualTo(10L);
        assertThat(tokens.accessToken()).isEqualTo("jwt");
    }

    @Test
    void rotatesAnActiveSessionAndRevokesThePreviousOne() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(10L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));
        when(sessionRepository.revokeIfActive(10L, TestFixtures.NOW)).thenReturn(true);
        givenNewSessionSaved(20L);
        when(tokenIssuer.issue(TestFixtures.user(), 20L))
                .thenReturn(new IssuedToken("jwt-novo", 900L));

        SessionTokens tokens = service.rotate(RAW_TOKEN, "10.0.0.1", "curl");

        assertThat(tokens.sessionId()).isEqualTo(20L);
        verify(sessionRepository).revokeIfActive(10L, TestFixtures.NOW);
        verify(sessionRepository, never()).revokeAllByUserId(anyLong(), any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void reuseOfARotatedTokenBringsDownEverySessionAndIsAudited() {
        givenHash();
        AuthSession consumed = revokedSession(10L);
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(consumed));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "10.0.0.2", "scanner"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Sessão expirada ou revogada. Autentique-se novamente.");

        verify(sessionRepository).revokeAllByUserId(1L, TestFixtures.NOW);
        verify(auditService).record(eq("admin@redekrill.com"), eq(AuditAction.REFRESH_TOKEN_REUSE),
                eq("AuthSession"), eq(10L), contains("foram revogadas"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void losingTheRevocationRaceIsTreatedAsReuse() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(10L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));
        when(sessionRepository.revokeIfActive(10L, TestFixtures.NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "10.0.0.2", "scanner"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Sessão já utilizada. Autentique-se novamente.");

        verify(sessionRepository).revokeAllByUserId(1L, TestFixtures.NOW);
        verify(auditService).record(eq("admin@redekrill.com"), eq(AuditAction.REFRESH_TOKEN_REUSE),
                eq("AuthSession"), eq(10L), any());
    }

    @Test
    void expiredSessionIsRejectedWithoutCascadeRevocation() {
        givenHash();
        AuthSession expired = new AuthSession(
                10L, 1L, TOKEN_HASH,
                TestFixtures.NOW.minus(REFRESH_EXPIRATION).minusSeconds(1),
                TestFixtures.NOW.minusSeconds(1), null, null, null);
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "10.0.0.1", "curl"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Sessão expirada ou revogada. Autentique-se novamente.");

        verify(sessionRepository, never()).revokeAllByUserId(anyLong(), any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void unknownTokenIsRejected() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "10.0.0.1", "curl"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Sessão expirada ou revogada. Autentique-se novamente.");
    }

    @Test
    void sessionOfAnInactiveUserCannotBeRotated() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(10L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.inactiveUser()));

        assertThatThrownBy(() -> service.rotate(RAW_TOKEN, "10.0.0.1", "curl"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário inativo. Autentique-se novamente.");

        verify(sessionRepository, never()).revokeIfActive(anyLong(), any());
    }

    @Test
    void blankTokenIsRejectedWithoutTouchingTheRepository() {
        assertThatThrownBy(() -> service.rotate(" ", "10.0.0.1", "curl"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Sessão não informada.");

        verify(sessionRepository, never()).findByRefreshTokenHash(any());
    }

    @Test
    void rejectsRefreshExpirationNotPositive() {
        assertThatThrownBy(() -> new AuthSessionService(
                sessionRepository, userRepository, tokenIssuer, refreshTokenGenerator,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC),
                Duration.ZERO, auditService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT_REFRESH_EXPIRATION deve ser maior que zero.");
    }

    @Test
    void logoutRevokesOnlyTheSessionThatPresentedTheToken() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(10L)));

        service.revokeRefreshToken(RAW_TOKEN);

        verify(sessionRepository).revokeIfActive(10L, TestFixtures.NOW);
        verify(sessionRepository, never()).revokeAllByUserId(anyLong(), any());
    }

    @Test
    void logoutIgnoresTokenWithoutSession() {
        givenHash();
        when(sessionRepository.findByRefreshTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        service.revokeRefreshToken(RAW_TOKEN);

        verify(sessionRepository, never()).revokeIfActive(anyLong(), any());
    }

    @Test
    void activeSessionCheckFollowsRevocationAndExpiry() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(activeSession(10L)));
        assertThat(service.isActive(10L)).isTrue();

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(revokedSession(10L)));
        assertThat(service.isActive(10L)).isFalse();

        assertThat(service.isActive(null)).isFalse();
    }

    private void givenHash() {
        when(refreshTokenGenerator.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    }

    private void givenNewSessionSaved(Long id) {
        when(refreshTokenGenerator.generate()).thenReturn("novo-token-gerado");
        when(refreshTokenGenerator.hash("novo-token-gerado")).thenReturn("hash-do-novo-token");
        when(sessionRepository.save(any(AuthSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, AuthSession.class).withId(id));
    }

    private static AuthSession activeSession(Long id) {
        return new AuthSession(id, 1L, TOKEN_HASH,
                TestFixtures.NOW.minusSeconds(60),
                TestFixtures.NOW.plusSeconds(600),
                null, "10.0.0.1", "curl");
    }

    private static AuthSession revokedSession(Long id) {
        return new AuthSession(id, 1L, TOKEN_HASH,
                TestFixtures.NOW.minusSeconds(60),
                TestFixtures.NOW.plusSeconds(600),
                TestFixtures.NOW.minusSeconds(1), "10.0.0.1", "curl");
    }
}
