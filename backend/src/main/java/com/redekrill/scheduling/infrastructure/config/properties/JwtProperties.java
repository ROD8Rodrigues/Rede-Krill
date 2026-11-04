/*
 * Segredo e validade do token de acesso.
 *
 * secret não é @NotBlank e isso é escolha, não esquecimento: quem decide o que fazer com
 * a ausência é JwtTokenService, que sabe em qual perfil está. Em dev e test ele gera um
 * segredo efêmero e avisa no log; em produção o boot é cancelado. Validar aqui mataria a
 * conveniência sem ganhar nada em segurança.
 *
 * O valor precisa ser Base64 com material suficiente para HMAC-SHA256. Quem confere isso
 * são os consumidores: o validator de produção decodifica e exige pelo menos 32 bytes, e
 * fora dele Keys.hmacShaKeyFor recusa chave curta com WeakKeyException — checar depois do
 * decode é o que pega segredo comprido em caracteres e curto em bytes.
 *
 * expiration é a vida do access token, e o service exige que seja maior que zero: token
 * que nasce vencido não autentica nada e o defeito apareceria como login quebrado.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Leitura de app.jwt. Não valida o conteúdo do segredo — a decisão depende do perfil e
 * fica em JwtTokenService e no validator de produção.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @NotNull Duration expiration
) {
}
