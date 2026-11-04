/*
 * Dono do ciclo de vida da sessão: abrir, renovar, revogar e limpar.
 *
 * Este serviço existe porque login não é um evento só: a cada acesso nasce uma linha
 * em AuthSession, o access token leva o id dela e o refresh token é a chave de
 * renovação. As operações precisam ver a mesma tabela com as mesmas regras, e
 * espalhá-las pelos casos de uso produziria três leituras diferentes do que "sessão
 * válida" significa.
 *
 * O ponto mais delicado é rotate, e ele está comentado por dentro: a revogação da
 * família acontece dentro da transação, e o registro do reuso mais a recusa, do lado
 * de fora. Fazer tudo no mesmo escopo é o jeito de perder a revogação — o rollback
 * desfaria justamente a escrita que precisa sobreviver ao evento.
 *
 * A classe não tem @Service nem @Component. É montada em
 * infrastructure.config.UseCaseConfig, igual aos casos de uso, porque recebe o tempo
 * de vida do refresh por propriedade e porque a camada de aplicação não deve nada ao
 * Spring.
 */
package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.RefreshTokenGenerator;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuthSession;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class AuthSessionService {

    private static final String SESSION_INVALID = "Sessão expirada ou revogada. Autentique-se novamente.";

    /**
     * Resultado da rotação. A recusa é devolvida para fora da transação porque o reuso
     * de token revogou a família inteira: lançar dentro do mesmo escopo apagaria
     * justamente a revogação que precisa sobreviver ao evento.
     */
    private record Rotation(SessionTokens tokens, AuthSession reused, String rejection) {

        private static Rotation accepted(SessionTokens tokens) {
            return new Rotation(tokens, null, null);
        }

        private static Rotation rejected(String rejection) {
            return new Rotation(null, null, rejection);
        }

        private static Rotation reused(AuthSession session, String rejection) {
            return new Rotation(null, session, rejection);
        }
    }

    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final Duration refreshExpiration;
    private final AuditService auditService;

    public AuthSessionService(AuthSessionRepository sessionRepository,
                              UserRepository userRepository,
                              TokenIssuer tokenIssuer,
                              RefreshTokenGenerator refreshTokenGenerator,
                              TransactionRunner transactionRunner,
                              Clock clock,
                              Duration refreshExpiration,
                              AuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.refreshExpiration = refreshExpiration;
        this.auditService = auditService;
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
        Rotation rotation = transactionRunner.execute(
                () -> rotateInTransaction(rawRefreshToken, clientIp, userAgent));

        if (rotation.reused() != null) {
            registerReuse(rotation.reused());
        }
        if (rotation.rejection() != null) {
            throw new AuthenticationFailedException(rotation.rejection());
        }
        return rotation.tokens();
    }

    private Rotation rotateInTransaction(String rawRefreshToken, String clientIp, String userAgent) {
        Instant now = Instant.now(clock);
        String hash = refreshTokenGenerator.hash(rawRefreshToken);
        AuthSession existing = sessionRepository.findByRefreshTokenHash(hash).orElse(null);
        if (existing == null) {
            return Rotation.rejected(SESSION_INVALID);
        }
        if (existing.revoked()) {
            // Reuso: o token já foi consumido por uma rotação ou por logout, então quem o
            // apresenta não é o portador legítimo. Derruba-se a família inteira.
            sessionRepository.revokeAllByUserId(existing.userId(), now);
            return Rotation.reused(existing, SESSION_INVALID);
        }
        if (!existing.activeAt(now)) {
            return Rotation.rejected(SESSION_INVALID);
        }

        User user = userRepository.findById(existing.userId())
                .filter(User::active)
                .orElse(null);
        if (user == null) {
            return Rotation.rejected("Usuário inativo. Autentique-se novamente.");
        }
        if (!sessionRepository.revokeIfActive(existing.id(), now)) {
            // Corrida: outra rotação consumiu a mesma sessão entre a leitura e o update.
            sessionRepository.revokeAllByUserId(existing.userId(), now);
            return Rotation.reused(existing, "Sessão já utilizada. Autentique-se novamente.");
        }
        return Rotation.accepted(createSession(user, clientIp, userAgent));
    }

    private void registerReuse(AuthSession session) {
        userRepository.findById(session.userId()).ifPresent(user -> auditService.record(
                user.email(), AuditAction.REFRESH_TOKEN_REUSE, "AuthSession", session.id(),
                "Token de refresh reapresentado depois de consumido; "
                        + "todas as sessões do usuário foram revogadas."));
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
