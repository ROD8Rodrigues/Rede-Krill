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
