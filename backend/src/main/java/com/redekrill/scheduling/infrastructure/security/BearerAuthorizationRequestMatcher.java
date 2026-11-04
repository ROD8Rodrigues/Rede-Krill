/*
 * Reconhece a requisição que chega com Authorization: Bearer.
 *
 * A comparação ignora caixa e olha apenas os sete primeiros caracteres porque o prefixo
 * pode vir em caixa alta e o token em outro formato — não é preciosismo, é o que HTTP
 * permite. Quem consome esta classe é a config de CSRF, usando o resultado para decidir
 * quando a checagem do token CSRF não se aplica.
 */
package com.redekrill.scheduling.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Requisições autenticadas por Bearer não são suscetíveis a CSRF, pois o token
 * não é anexado automaticamente pelo navegador. Essa exceção permite o uso do
 * Swagger em desenvolvimento sem enfraquecer a proteção da autenticação por
 * cookie, que continua exigindo o token CSRF.
 */
public class BearerAuthorizationRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.regionMatches(
                true, 0, "Bearer ", 0, "Bearer ".length());
    }
}
