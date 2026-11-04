/*
 * A origem permitida para o frontend chamar a API.
 *
 * É lista e não string única porque o mesmo backend costuma precisar do origin do Vite em
 * dev (localhost:5173 é o valor padrão) e do domínio real em produção, e o nome do host
 * não deve morar no código. O @NotEmpty existe porque lista vazia é configurador digitando
 * pouco: com ela, o navegador passa a receber falha de CORS em tudo e o sintoma parece um
 * bug de rede, não de ambiente.
 *
 * Só o origin mora aqui. Verbos, cabeçalhos e max-age estão escritos em SecurityConfig,
 * porque mudar um cabeçalho autorizado é decisão de segurança, e decisão de segurança
 * deve aparecer em diff de código, não em variável de ambiente.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Lista de origens que podem chamar a API com credenciais. Validada no boot: sem nenhuma
 * origem, o backend sobe mudo para o frontend e a falha aparece longe de quem causou.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins
) {
}
