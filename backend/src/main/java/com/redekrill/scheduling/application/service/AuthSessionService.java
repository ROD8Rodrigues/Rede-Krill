package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.RefreshTokenGenerator;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class AuthSessionService {

    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final Duration refreshExpiration;

    public AuthSessionService(AuthSessionRepository sessionRepository,
                              UserRepository userRepository,
                              TokenIssuer tokenIssuer,
                              RefreshTokenGenerator refreshTokenGenerator,
                              TransactionRunner transactionRunner,
                              Clock clock,
                              Duration refreshExpiration) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.refreshExpiration = refreshExpiration;
        if (refreshExpiration == null || refreshExpiration.isZero() || refreshExpiration.isNegative()) {
            throw new IllegalArgumentException("JWT_REFRESH_EXPIRATION deve ser maior que zero.");
        }
    }

    public SessionTokens start(User user, String clientIp, String userAgent) {
        return transactionRunner.execute(() -> createSession(user, clientIp, userAgent));
    }

    public SessionTokens rotate(String rawRefreshToken, String clientIp, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationFailedException("Sessão não informada.");
        }
        return transactionRunner.execute(() -> {
            Instant now = Instant.now(clock);
            String hash = refreshTokenGenerator.hash(rawRefreshToken);
            AuthSession existing = sessionRepository.findByRefreshTokenHash(hash)
                    .filter(session -> session.activeAt(now))
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Sessão expirada ou revogada. Autentique-se novamente."));

            User user = userRepository.findById(existing.userId())
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário inativo. Autentique-se novamente."));

            if (!sessionRepository.revokeIfActive(existing.id(), now)) {
                throw new AuthenticationFailedException(
                        "Sessão já utilizada. Autentique-se novamente.");
            }
            return createSession(user, clientIp, userAgent);
        });
    }

    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        transactionRunner.execute(() -> {
            Instant now = Instant.now(clock);
            sessionRepository.findByRefreshTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                    .ifPresent(session -> sessionRepository.revokeIfActive(session.id(), now));
            return null;
        });
    }

    public void revokeSession(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        transactionRunner.execute(() -> {
            sessionRepository.revokeIfActive(sessionId, Instant.now(clock));
            return null;
        });
    }

    public void revokeAllForUser(Long userId) {
        transactionRunner.execute(() -> {
            sessionRepository.revokeAllByUserId(userId, Instant.now(clock));
            return null;
        });
    }

    public boolean isActive(Long sessionId) {
        if (sessionId == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        return sessionRepository.findById(sessionId)
                .filter(session -> session.activeAt(now))
                .isPresent();
    }

    private SessionTokens createSession(User user, String clientIp, String userAgent) {
        Instant now = Instant.now(clock);
        String rawRefreshToken = refreshTokenGenerator.generate();
        AuthSession session = sessionRepository.save(AuthSession.create(
                user.id(),
                refreshTokenGenerator.hash(rawRefreshToken),
                now,
                now.plus(refreshExpiration),
                clientIp,
                userAgent
        ));

        IssuedToken accessToken = tokenIssuer.issue(user, session.id());
        return new SessionTokens(
                accessToken.value(),
                accessToken.expiresInSeconds(),
                rawRefreshToken,
                refreshExpiration.toSeconds(),
                session.id(),
                UserResult.from(user)
        );
    }
}
