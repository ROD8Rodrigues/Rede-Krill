/*
 * Os sete ajustes de autenticação que a apresentação precisa conhecer.
 *
 * Montado uma vez em infrastructure.security.SecurityConfig, a partir de
 * AuthProperties e SecurityProperties, e injetado em AuthController. O record mora na
 * aplicação justamente para isso: o controller lê nomes de cookie, flag de exposição
 * do token e política de cabeçalho proxy sem importar nada de infraestrutura.
 *
 * Dois campos decidem comportamento de segurança, e os dois vêm desligados por padrão.
 * exposeTokenInResponse é falso, então o corpo da resposta não leva access token — só
 * os cookies HttpOnly. trustForwardedHeaders é falso, e o ClientIpResolver passa a ler
 * X-Forwarded-For quando vira verdadeiro: atrás de um proxy é preciso ligar, mas com a
 * porta aberta o próprio cliente inventa o endereço de origem, e o bloqueio por IP do
 * login conta tentativas contra uma fantasia.
 */
package com.redekrill.scheduling.application.dto.auth;

/**
 * Configuração de cookies e de identificação de cliente necessária ao
 * controlador de autenticação. Isolada da infraestrutura para preservar a
 * direção das dependências entre as camadas.
 */
public record AuthCookieSettings(
        String accessCookieName,
        String refreshCookieName,
        boolean cookieSecure,
        String cookieSameSite,
        String cookieDomain,
        boolean exposeTokenInResponse,
        boolean trustForwardedHeaders
) {
}
