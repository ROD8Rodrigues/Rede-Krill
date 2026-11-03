package com.redekrill.scheduling.infrastructure.security;

import com.redekrill.scheduling.application.dto.auth.AuthRequestAttributes;
import com.redekrill.scheduling.application.dto.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

/**
 * Bloqueia o uso normal da conta enquanto a senha inicial não for trocada.
 * A lista de exceções permite consultar o próprio usuário, sair e trocar a senha.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/logout",
            "/api/auth/change-password",
            "/api/auth/csrf"
    );

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object mustChange = request.getAttribute(AuthRequestAttributes.MUST_CHANGE_PASSWORD);

        if (authentication != null && authentication.isAuthenticated()
                && Boolean.TRUE.equals(mustChange)
                && !ALLOWED_PATHS.contains(request.getRequestURI())
                && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), new ApiError(
                    Instant.now(),
                    HttpServletResponse.SC_FORBIDDEN,
                    "Forbidden",
                    "É necessário trocar a senha inicial antes de continuar.",
                    request.getRequestURI(),
                    null,
                    "PASSWORD_CHANGE_REQUIRED"
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
