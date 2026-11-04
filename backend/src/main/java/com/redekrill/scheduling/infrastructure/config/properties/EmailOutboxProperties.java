/*
 * Volume e cadência do processador de e-mails.
 *
 * enabled desliga o processamento inteiro — o outbox continua acumulando mensagens e
 * nada é enviado. É o que se faz em ambiente de demonstração com SMTP apontado para
 * servidor de verdade, sem querer apagar a tabela.
 *
 * maxAttempts é o limite antes de a mensagem virar FAILED em definitivo. batchSize
 * limita quantas são puxadas por ciclo, e @Min(1) protege do caso óbvio: lote de
 * tamanho zero significa fila parada com o job rodando feliz.
 *
 * pollIntervalMs tem piso de 1000 porque a consulta de mensagens vencidas é uma leitura
 * no banco com lock pessimista; intervalos menores que um segundo virariam competição
 * entre réplicas por nada. initialBackoffSeconds é a base da dobra exponencial que o
 * processador aplica a cada falha.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ajustes do envio assíncrono: ativação, limite de tentativas, cadência e espaçamento.
 * Não guarda credencial de SMTP — isso continua em spring.mail, do próprio Boot.
 */
@Validated
@ConfigurationProperties(prefix = "app.email.outbox")
public record EmailOutboxProperties(
        boolean enabled,
        @Min(1) int maxAttempts,
        @Min(1000) long pollIntervalMs,
        @Min(1) long initialBackoffSeconds,
        @Min(1) int batchSize
) {
}
