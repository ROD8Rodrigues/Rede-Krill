/*
 * Executa a trava de senha inicial que o JwtAuthenticationFilter sinaliza.
 *
 * O atributo MUST_CHANGE_PASSWORD vem da request, gravado durante a autenticação. Aqui
 * não há nova consulta ao banco: é um filtro lendo decisão já tomada. Quando a decisão é
 * "troque a senha" e o caminho não está na lista de exceções, a resposta é 403 com code
 * PASSWORD_CHANGE_REQUIRED e o controller nem chega a rodar.
 *
 * O code existe porque ACCESS_DENIED também é 403. Sem ele, distinguir os dois casos
 * dependeria de ler a frase em português da mensagem.
 *
 * Quatro rotas ficam liberadas — me, logout, change-password e csrf — porque um bloqueio
 * absoluto tiraria da pessoa justamente os meios de resolver o bloqueio. OPTIONS passa
 * junto: preflight não altera estado e derrubá-lo viraria erro de CORS.
 */
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
