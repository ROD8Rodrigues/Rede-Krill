/*
 * Controle de tentativas de login, com bloqueio temporário e progressivo.
 *
 * Duas contagens independentes andam juntas: por conta e por endereço de origem. A
 * primeira impede adivinhação contra um e-mail conhecido; a segunda impede o
 * espalhamento — quem testa uma senha em dez mil contas nunca estoura o limite de
 * nenhuma delas, mas estoura o do próprio IP.
 *
 * O estado vive em memória, e a Javadoc da classe já diz por que isso limita o
 * componente a uma instância. O teto de chaves rastreadas existe porque a tabela é
 * aberta por quem ataca: sem limite, uma avalanche de e-mails aleatórios faria o mapa
 * crescer até faltar memória. O preço de limitar é honesto — liberar espaço pode
 * descartar um bloqueio em andamento de outra conta.
 *
 * O tempo de bloqueio cresce em níveis, até quatro vezes o configurado, em vez de
 * virar permanente. Lockout sem fim é porta para negação de serviço contra o usuário
 * legítimo: quem erra a senha três vezes depois de esquecê-la ficaria travado para
 * sempre.
 */
package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.exception.TooManyRequestsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
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

    /**
     * Teto de chaves rastreadas por mapa. Sem ele, uma avalanche de tentativas com
     * e-mails aleatórios faria o estado crescer sem limite; ao atingir o teto, entradas
     * vencidas são removidas e, se ainda assim não houver espaço, a mais antiga cede.
     */
    private static final int MAX_TRACKED_KEYS = 10_000;

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
        if (!attempts.containsKey(key)) {
            makeRoom(attempts, now);
        }
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
        if (accountAttempts.size() + ipAttempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        makeRoom(accountAttempts, now);
        makeRoom(ipAttempts, now);
    }

    /**
     * Libera espaço antes de inserir uma chave nova: descarta o que já venceu e, se o mapa
     * continua cheio, remove uma entrada qualquer. A escolha arbitrária pode encerrar um
     * bloqueio temporário de outra conta — troca aceita para manter a memória limitada.
     */
    private void makeRoom(Map<String, AttemptState> attempts, Instant now) {
        if (attempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        attempts.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                prune(entry.getValue(), now);
                return entry.getValue().failures.isEmpty() && entry.getValue().lockedUntil == null;
            }
        });
        if (attempts.size() < MAX_TRACKED_KEYS) {
            return;
        }
        Iterator<Map.Entry<String, AttemptState>> entries = attempts.entrySet().iterator();
        if (entries.hasNext()) {
            entries.next();
            entries.remove();
        }
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
