/*
 * Um identificador por request, do começo ao fim.
 *
 * A ordem é HIGHEST_PRECEDENCE para que o id exista antes de qualquer outro filtro ou
 * serviço escrever log — senão as primeiras linhas da request, justamente as que explicam
 * um 401, saem sem o rastro.
 *
 * Se o cliente manda X-Correlation-ID, o valor só é aproveitado quando casa com o padrão
 * [A-Za-z0-9._-] de 8 a 64 caracteres. A validação não é capricho: header vai para o log,
 * e aceitar texto livre permitiria injetar quebras de linha ou uma string enorme em
 * cada linha de cada request. Fora do padrão, gera-se um UUID e a request ainda ganha
 * resposta com id.
 *
 * O valor volta no cabeçalho da resposta, permitindo colar no ticket a id exatamente como
 * ela aparece no log. A chave sai do MDC no finally porque a thread do container é
 * reusada — sobrar um correlationId no MDC faz a próxima request herdar o rastro alheia,
 * e isso é pior do que não ter rastro nenhum.
 */
package com.redekrill.scheduling.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtro de observabilidade: escreve correlationId no MDC e devolve o mesmo valor no
 * cabeçalho. Não autentica nem decide nada sobre a requisição; acompanha, só.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || !SAFE_ID.matcher(correlationId).matches()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
