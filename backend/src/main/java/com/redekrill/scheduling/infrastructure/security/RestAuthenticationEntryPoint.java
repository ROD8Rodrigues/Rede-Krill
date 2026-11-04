/*
 * A resposta de 401 do sistema, escrita antes de qualquer controller.
 *
 * Cai aqui quem chega sem token, com token inválido ou com sessão revogada — situações
 * diferentes que, para o cliente, significam a mesma coisa: não há identidade válida
 * naquela requisição. O corpo é o ApiError de sempre, com code UNAUTHENTICATED.
 *
 * A tentativa também é auditada, com action ACCESS_DENIED (a mesma do 403) e ator
 * anonimo@redekrill.local quando não há identidade na request. Falha na auditoria não
 * barra a resposta: o erro é logado e o 401 sai mesmo assim, porque negar acesso por
 * causa de um log indisponível criaria um defeito novo.
 */
package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.application.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Ponto de entrada para requisição não autenticada: 401 em JSON e trilha de auditoria.
 * Não decide quais rotas exigem login — isso está na SecurityFilterChain.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(RestAuthenticationEntryPoint.class);
    private static final String ANONYMOUS_ACTOR = "anonimo@redekrill.local";

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        recordUnauthorized(request);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiError error = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Autenticação necessária.",
                request.getRequestURI(),
                null,
                "UNAUTHENTICATED"
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private void recordUnauthorized(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actor = authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getName())
                    ? ANONYMOUS_ACTOR
                    : authentication.getName();
            auditService.record(actor, AuditAction.ACCESS_DENIED, "Endpoint", null,
                    "HTTP 401: %s %s".formatted(request.getMethod(), request.getRequestURI()));
        } catch (RuntimeException exception) {
            log.error("Falha ao registrar auditoria de HTTP 401 para {} {}.",
                    request.getMethod(), request.getRequestURI(), exception);
        }
    }
}
