package com.redekrill.scheduling.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.model.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        recordAccessDenied(request);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", "Você não tem permissão para executar esta ação.");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }

    private void recordAccessDenied(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actorEmail = authentication == null || !authentication.isAuthenticated()
                    ? "anonimo"
                    : authentication.getName();
            auditService.record(
                    actorEmail,
                    AuditAction.ACCESS_DENIED,
                    "Endpoint",
                    null,
                    "Acesso negado: %s %s".formatted(request.getMethod(), request.getRequestURI())
            );
        } catch (RuntimeException exception) {
            log.warn("Não foi possível registrar o evento ACCESS_DENIED para {} {}.",
                    request.getMethod(), request.getRequestURI(), exception);
        }
    }
}
