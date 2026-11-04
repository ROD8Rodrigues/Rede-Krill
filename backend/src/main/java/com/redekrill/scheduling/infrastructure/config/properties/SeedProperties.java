/*
 * Quem cria a primeira conta administrativa, se alguém deve criar.
 *
 * O padrão do projeto é deixar o seed ligado em dev e desligado em produção: sem ele,
 * ninguém consegue chamar a API antes de existir um ADMIN. enabled é o interruptor, e o
 * DataInitializer trata a ausência de senha de acordo com o perfil — em produção é erro,
 * alhures é aviso no log.
 *
 * adminEmail passa por trim e caixa baixa antes de qualquer uso, igual ao que a
 * autenticação faz, porque o índice único da tabela é sobre o e-mail normalizado; se o
 * seed consultasse com a grafia crua, daria "não existe" e criaria duplicado.
 *
 * requirePasswordChange define se a conta nasce com a trava de senha inicial. Ligada, é
 * o comportamento que se quer em produção: ninguém opera com a senha que veio de variável
 * de ambiente.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parâmetros do usuário inicial. A força da senha não é campo daqui: quem confere é o
 * PasswordPolicy, no momento em que a conta é criada.
 */
@Validated
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        String adminPassword,
        boolean requirePasswordChange
) {
}
