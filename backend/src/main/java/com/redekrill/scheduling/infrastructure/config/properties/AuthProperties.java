/*
 * Configuração de autenticação que a apresentação e a segurança compartilham.
 *
 * Os nomes das duas cookies são configuração porque o frontend precisa conhecê-los, e a
 * leitura aqui evita que a string apareça escrita em dois lugares com grafias diferentes.
 * cookieSecure e cookieSameSite controlam como o navegador trata esses valores: em
 * produção secure precisa ser true, e o validator de produção exige isso.
 *
 * cookieDomain é o único campo sem validação de preenchimento — fica vazio quando a
 * cookie deve valer para o host exato que serviu a resposta, que é o comportamento
 * padrão e o mais restrito.
 *
 * exposeTokenInResponse é a chave do modo "devolve o token no corpo" para clientes sem
 * cookie (app mobile, Postman). Em produção o validator exige false, porque token no
 * corpo é token que fica em log de aplicação intermediária.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Propriedades de sessão e cookies, com o tempo de vida do refresh. Registro validado na
 * subida: nome de cookie vazio derruba o boot em vez de gerar cookie descartada.
 */
@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @NotBlank String accessCookieName,
        @NotBlank String refreshCookieName,
        boolean cookieSecure,
        @NotBlank String cookieSameSite,
        String cookieDomain,
        boolean exposeTokenInResponse,
        @NotNull Duration refreshExpiration
) {
}
