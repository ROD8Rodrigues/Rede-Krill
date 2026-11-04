/*
 * Faxina das sessões de refresh que já venceram.
 *
 * O delete pega linhas com expiresAt anterior a "agora menos
 * app.security.session-expired-retention", então quem ainda pode rotacionar um refresh
 * nunca é tocado — a janela de retenção existe para o histórico recente continuar
 * consultável. Sem este job, a tabela cresceria para sempre com linhas que nenhuma
 * decisão de segurança lê.
 *
 * O intervalo vem de app.security.session-cleanup-interval (12h, e é o mesmo valor usado
 * como atraso inicial: a primeira execução só acontece depois de um ciclo, não no boot).
 * app.security.session-cleanup-enabled desliga o bean por completo — basta uma
 * configuração explícita com valor diferente de true para ele nem ser registrado.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.domain.repository.AuthSessionRepository;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * A rotação de refresh só aceita sessão ativa, então nenhuma decisão de segurança depende
 * das linhas vencidas. O job roda em todas as réplicas e o expurgo é idempotente.
 */
@Component
@ConditionalOnProperty(name = "app.security.session-cleanup-enabled", havingValue = "true",
        matchIfMissing = true)
public class AuthSessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuthSessionCleanupJob.class);

    private final AuthSessionRepository sessionRepository;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public AuthSessionCleanupJob(AuthSessionRepository sessionRepository,
                                 SecurityProperties securityProperties,
                                 Clock clock) {
        this.sessionRepository = sessionRepository;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.security.session-cleanup-interval:12h}",
            initialDelayString = "${app.security.session-cleanup-interval:12h}")
    public void purgeExpiredSessions() {
        Instant cutoff = Instant.now(clock).minus(securityProperties.sessionExpiredRetention());
        int removed = sessionRepository.deleteExpiredBefore(cutoff);
        if (removed > 0) {
            log.info("Expurgadas {} sessões de refresh vencidas antes de {}.", removed, cutoff);
        }
    }
}
