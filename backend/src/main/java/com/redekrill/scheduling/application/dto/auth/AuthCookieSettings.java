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
