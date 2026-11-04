/*
 * O catálogo que o Swagger mostra, escrito à mão em vez de depender do arquivo yaml.
 *
 * Title, descrição e versão ficam aqui porque são decisão de projeto, não de ambiente.
 * O esquema bearerAuth com formato JWT é o que faz o botão "Authorize" aparecer na UI e
 * permite testar endpoint autenticado sem precisar de cookie de navegador — útil em dev,
 * já que a request sai com Authorization: Bearer e o CSRF nem é checado nesse caminho.
 * O cookieAuth descreve a outra metade do fluxo real: as rotas de sessão mandam o cookie
 * de acesso, e sem esse esquema o `@SecurityRequirement(name = "cookieAuth")` do
 * AuthController apontaria para um esquema inexistente no documento. O nome vem do mesmo
 * ajuste que o controller usa, então a documentação não diverge se o cookie for renomeado.
 *
 * Este bean não tem poder nenhum sobre segurança real: quem decide acesso é a
 * SecurityFilterChain. Ele só descreve o contrato para quem está lendo a documentação.
 */
package com.redekrill.scheduling.infrastructure.config;

import com.redekrill.scheduling.application.dto.auth.AuthCookieSettings;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Personaliza o documento OpenAPI exposto pelo springdoc. Não publica rota nova nem muda
 * resposta de endpoint — é metadado sobre o que já existe.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Monta o documento OpenAPI com os dois esquemas de autenticação que a API aceita.
     *
     * @param authCookieSettings nomes de cookie em uso pela apresentação, para que o esquema
     *                           descrito seja o mesmo que o navegador realmente envia
     * @return o objeto {@code OpenAPI} que o springdoc serializa em {@code /v3/api-docs}
     */
    @Bean
    public OpenAPI customOpenAPI(AuthCookieSettings authCookieSettings) {
        return new OpenAPI()
                .info(new Info()
                        .title("Rede Krill - Agendamento de Caminhões")
                        .description("API do desafio de agendamento de caminhões da Rede Krill")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(authCookieSettings.accessCookieName())));
    }
}
