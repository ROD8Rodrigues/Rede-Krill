/*
 * O remetente dos e-mails transacionais, sozinho.
 *
 * Um campo só, mas validado como endereço de verdade: se o "from" vier malformado, o
 * SMTP recusa lá na tentativa de envio, dentro do laço de reteste do outbox, e o log
 * mostra erro de servidor de mail. Checar aqui transforma isso em falha de boot, com o
 * nome da propriedade na mensagem.
 *
 * O endereço precisa bater com o que spring.mail.username autoriza; servidor que exige
 * remetente autenticado recusa qualquer outro valor, e essa recusa não tem nada a ver
 * com o código da aplicação.
 */
package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de conteúdo do e-mail, hoje apenas o remetente. Sem host nem senha de
 * SMTP: o envio usa a configuração padrão do starter de mail do Spring.
 */
@Validated
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
        @NotBlank @Email String from
) {
}
