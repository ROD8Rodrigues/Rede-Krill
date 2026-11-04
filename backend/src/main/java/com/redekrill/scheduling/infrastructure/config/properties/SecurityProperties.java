/*
 * Os números que definem o rigor da segurança: bloqueio de login, retenção de sessão e
 * custo do hash de senha.
 *
 * Os dois máximos de tentativa são separados por propósito: um por conta e um por IP. Só
 * o por conta permite ataque distribuído derrubando a conta de um executivo; só o por IP
 * permite pulverizar tentativas em várias contas sem nunca bloquear ninguém. Os dois
 * juntos fecham os dois lados.
 *
 * loginWindow é o relógio que esquece as falhas — fora dele, tentativas antigas não
 * contam mais; loginLockout é o tempo de espera depois de estourar. sessionExpired-retention
 * mantém no banco sessões já vencidas por um período antes do expurgo, para que a
 * auditoria consiga apontar o que aconteceu em uma sessão recente.
 *
 * A validação extra no construtor compacto existe porque Duration aceita valor negativo:
 * retenção negativa faria o job de limpeza apagar sessões que ainda nem venceram.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Limites de proteção de login e de ciclo de vida de sessão. Não guarda regra de
 * negócio; guarda os parâmetros que as políticas do domínio usam para decidir.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean trustForwardedHeaders,
        @Min(1) int loginMaxAttemptsPerAccount,
        @Min(1) int loginMaxAttemptsPerIp,
        @NotNull Duration loginWindow,
        @NotNull Duration loginLockout,
        @NotNull Duration sessionExpiredRetention,

        // Work factor do BCrypt. Antes não havia exposição por configuração e o encoder
        // ficava preso no padrão 10. Elevar o fator não invalida hashes existentes: o custo
        // vai embutido no próprio hash e o BCrypt lê o valor de cada um. O piso em 10 é
        // deliberado — abaixo disso a tabela de custo já não segura uma tentativa em GPU.
        @Min(10) @Max(20) int bcryptStrength
) {
    public SecurityProperties {
        if (sessionExpiredRetention != null && sessionExpiredRetention.isNegative()) {
            throw new IllegalArgumentException(
                    "app.security.session-expired-retention deve ser maior ou igual a zero.");
        }
    }
}
