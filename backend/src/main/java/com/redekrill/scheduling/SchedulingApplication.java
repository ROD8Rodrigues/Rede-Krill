/*
 * A única classe que o Spring olha antes de qualquer outra.
 *
 * Está na raiz do pacote de `backend` de propósito: é daí que o component scan alcança
 * `infrastructure` e `presentation`, as duas camadas anotadas. `domain` e `application` não
 * precisam ser alcançadas porque não têm nenhuma anotação — elas entram pelo `UseCaseConfig`,
 * que faz `new` delas e entrega como bean.
 *
 * Os detalhes que ficam escondidos aqui dentro valem a leitura: os `@ConfigurationProperties`
 * não se registram sozinhos (não há `@ConfigurationPropertiesScan` no projeto) e os dois jobs
 * agendados só existem porque o agendamento foi ligado nesta classe.
 */

package com.redekrill.scheduling;

import com.redekrill.scheduling.infrastructure.config.properties.AuthProperties;
import com.redekrill.scheduling.infrastructure.config.properties.CorsProperties;
import com.redekrill.scheduling.infrastructure.config.properties.EmailOutboxProperties;
import com.redekrill.scheduling.infrastructure.config.properties.EmailProperties;
import com.redekrill.scheduling.infrastructure.config.properties.JwtProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SecurityProperties;
import com.redekrill.scheduling.infrastructure.config.properties.SeedProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da aplicação. Não há regra de negócio aqui e não deve haver.
 *
 * As três anotações fazem trabalho que nenhuma outra classe faria:
 * <ul>
 *   <li>{@code @SpringBootApplication} liga o component scan a partir deste pacote, a
 *       autoconfiguração do Spring Boot e o servidor Web MVC;</li>
 *   <li>{@code @EnableScheduling} é o que faz {@code EmailOutboxProcessor} e
 *       {@code AuthSessionCleanupJob} rodarem sozinhos depois do boot — sem ela os dois
 *       {@code @Scheduled} são métodos comuns que ninguém chama;</li>
 *   <li>{@code @EnableConfigurationProperties} registra as sete records de
 *       {@code infrastructure/config/properties} como beans. Elas não são anotadas com
 *       {@code @Component} justamente para continuarem sendo só dados de configuração, e a
 *       lista explícita aqui deixa visível o que o ambiente precisa fornecer.</li>
 * </ul>
 *
 * O que <b>não</b> deve morar nesta classe: beans de caso de uso (vão em
 * {@code UseCaseConfig}), regras de segurança (vão em {@code SecurityConfig}), filtros,
 * leitura de variável de ambiente fora de um record de properties e qualquer {@code main}
 * alternativo — teste de integração usa esta mesma classe, subir contexto paralelo geraria
 * duas versões do mesmo sistema.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        AuthProperties.class,
        SecurityProperties.class,
        CorsProperties.class,
        EmailProperties.class,
        EmailOutboxProperties.class,
        SeedProperties.class
})
public class SchedulingApplication {

    /**
     * Sobe o contexto e entrega o resto ao servidor Web embutido. {@code args} vai para o
     * {@code Environment} como propriedade de linha de comando, então
     * {@code --spring.profiles.active=prod} funciona por aqui sem nenhuma leitura manual.
     *
     * @param args parâmetros de linha de comando passados ao Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }
}
