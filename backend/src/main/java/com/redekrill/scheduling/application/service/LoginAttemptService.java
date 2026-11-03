package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção de login em memória, adequada a uma única instância. Para múltiplas
 * instâncias, o estado deve ser compartilhado (por exemplo, Redis). O bloqueio é
 * temporário e progressivo, evitando negação de serviço permanente.
 */
public class LoginAttemptService {

    private static final String UNKNOWN_IP = "desconhecido";

    private final Clock clock;
    private final int maxAttemptsPerAccount;
    private final int maxAttemptsPerIp;
    private final Duration window;
    private final Duration lockout;
    private final Map<String, AttemptState> accountAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> ipAttempts = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock, int maxAttemptsPerAccount, int maxAttemptsPerIp,
                               Duration window, Duration lockout) {
        this.clock = clock;
        this.maxAttemptsPerAccount = maxAttemptsPerAccount;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.window = window;
        this.lockout = lockout;
    }

    public void assertAllowed(String email, String clientIp) {
        Instant now = Instant.now(clock);
        check(accountAttempts.get(accountKey(email)), now);
        check(ipAttempts.get(ipKey(clientIp)), now);
        pruneIfNecessary(now);
    }

    public void recordFailure(String email, String clientIp) {
        Instant now = Instant.now(clock);
        registerFailure(accountAttempts, accountKey(email), maxAttemptsPerAccount, now);
        registerFailure(ipAttempts, ipKey(clientIp), maxAttemptsPerIp, now);
    }

    public void recordSuccess(String email) {
        accountAttempts.remove(accountKey(email));
    }

    private void check(AttemptState state, Instant now) {
        if (state == null) {
            return;
        }
        synchronized (state) {
            if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
                long seconds = Duration.between(now, state.lockedUntil).toSeconds();
                if (seconds < 1) {
                    seconds = 1;
                }
                throw new TooManyRequestsException(
                        "Muitas tentativas de login. Tente novamente mais tarde.", seconds);
            }
            prune(state, now);
        }
    }

    private void registerFailure(Map<String, AttemptState> attempts, String key,
                                 int threshold, Instant now) {
        AttemptState state = attempts.computeIfAbsent(key, ignored -> new AttemptState());
        synchronized (state) {
            prune(state, now);
            state.failures.addLast(now);
            if (state.failures.size() >= threshold) {
                int blockLevel = Math.min(4,
                        1 + (state.failures.size() - threshold) / Math.max(1, threshold));
                state.lockedUntil = now.plus(lockout.multipliedBy(blockLevel));
            }
        }
    }

    private void prune(AttemptState state, Instant now) {
        Instant threshold = now.minus(window);
        while (!state.failures.isEmpty() && !state.failures.peekFirst().isAfter(threshold)) {
            state.failures.removeFirst();
        }
        if (state.lockedUntil != null && !state.lockedUntil.isAfter(now)) {
            state.lockedUntil = null;
        }
    }

    private void pruneIfNecessary(Instant now) {
        if (accountAttempts.size() + ipAttempts.size() < 10_000) {
            return;
        }
        accountAttempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().failures.isEmpty() && entry.getValue().lockedUntil == null;
            }
        });
        ipAttempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().failures.isEmpty() && entry.getValue().lockedUntil == null;
            }
        });
    }

    private String accountKey(String email) {
        return email == null || email.isBlank()
                ? "desconhecido@redekrill.local"
                : email.trim().toLowerCase(Locale.ROOT);
    }

    private String ipKey(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? UNKNOWN_IP : clientIp.trim();
    }

    private static final class AttemptState {
        private final Deque<Instant> failures = new ArrayDeque<>();
        private Instant lockedUntil;
    }
}
