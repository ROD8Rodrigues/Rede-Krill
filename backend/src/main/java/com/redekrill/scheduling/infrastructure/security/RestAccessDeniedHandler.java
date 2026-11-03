package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.application.dto.error.ApiError;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);
    private static final String ANONYMOUS_ACTOR = "anonimo@redekrill.local";

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

        ApiError error = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Você não tem permissão para executar esta ação.",
                request.getRequestURI(),
                null,
                "ACCESS_DENIED"
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private void recordAccessDenied(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actor = authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getName())
                    ? ANONYMOUS_ACTOR
                    : authentication.getName();
            auditService.record(
                    actor,
                    AuditAction.ACCESS_DENIED,
                    "Endpoint",
                    null,
                    "HTTP 403: %s %s".formatted(request.getMethod(), request.getRequestURI())
            );
        } catch (RuntimeException exception) {
            log.error("Falha ao registrar auditoria de HTTP 403 para {} {}.",
                    request.getMethod(), request.getRequestURI(), exception);
        }
    }
}
