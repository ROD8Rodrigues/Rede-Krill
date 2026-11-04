/*
 * Um bean de Clock para toda a aplicação.
 *
 * Pode parecer exagero expor o relógio do sistema quando basta chamar Instant.now(), e é
 * justamente esse o ponto: toda decisão que depende de hora — expiração de sessão, janela
 * de bloqueio de login, carimbo da auditoria, vencimento da mensagem do outbox — lê o
 * Clock injetado, não o Instant.now() solto. Nos testes isso é a diferença entre um caso
 * de uso testável e um que só falha de madrugada: o teste entrega um relógio congelado e
 * escolhe se agora é terça de manhã ou véspera de feriado.
 *
 * As poucas chamadas a Instant.now() que restam estão em filtro de erro e no handler
 * global, onde o valor é só o carimbo do corpo da resposta — nada decide com base nele.
 *
 * É UTC de propósito, para bater com o que o banco devolve ao comparar Instant.
 */
package com.redekrill.scheduling.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Produz o relógio único que a aplicação injeta em quem precisa saber que horas são.
 * Não define política de horário de funcionamento nem de expiração — isso é configuração.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
