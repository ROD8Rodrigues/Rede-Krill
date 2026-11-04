/*
 * Como um agendamento vira um HTML enviado pelo SMTP.
 *
 * O corpo não é concatenação de strings: vem do template Thymeleaf
 * email/appointment-created, com dez variáveis mais a lista de warnings. Manter o
 * layout fora do código é o que permite ajustar o e-mail sem tocar em lógica, e o
 * Context com locale pt-BR faz data e quantidade saírem no formato que o fornecedor lê.
 *
 * A data é formatada como dd/MM/yyyy e o horário vem do label do time slot, ambos
 * gerados antes de chegar ao template — o template não deve calcular nada.
 *
 * O destinatário é sempre o e-mail do fornecedor gravado no agendamento, nunca o de um
 * usuário autenticado: quem precisa saber que a entrega foi marcada é quem vai receber o
 * caminhão.
 *
 * Qualquer falha de construção da mensagem ou de envio vira EmailDeliveryException com a
 * causa embaixo. O motivo do reenvio fica no log do processador; aqui só se garante que
 * uma exceção do Jakarta Mail não vaze nua para a camada de aplicação.
 */
package com.redekrill.scheduling.infrastructure.email;

import com.redekrill.scheduling.application.port.EmailNotifier;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.infrastructure.config.properties.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Implementa EmailNotifier com JavaMail e Thymeleaf. Não decide quando enviar nem se
 * deve retentar — a fila e a política de reteste pertencem ao processador de outbox.
 */
@Component
public class SmtpEmailNotifier implements EmailNotifier {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailProperties emailProperties;

    public SmtpEmailNotifier(JavaMailSender mailSender,
                             SpringTemplateEngine templateEngine,
                             EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailProperties = emailProperties;
    }

    @Override
    public void sendAppointmentConfirmation(Appointment appointment, List<String> warnings) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(emailProperties.from());
            helper.setTo(appointment.supplierEmail());
            helper.setSubject("Agendamento confirmado - Rede Krill");
            helper.setText(buildHtml(appointment, warnings), true);

            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new EmailDeliveryException(
                    "Falha ao enviar e-mail de confirmação do agendamento.", exception);
        }
    }

    private String buildHtml(Appointment appointment, List<String> warnings) {
        Context context = new Context(PT_BR);
        context.setVariable("orderId", appointment.orderId());
        context.setVariable("supplierName", appointment.supplierName());
        context.setVariable("supplierEmail", appointment.supplierEmail());
        context.setVariable("truckPlate", appointment.truckPlate());
        context.setVariable("formattedDate", appointment.date().format(DATE_FORMAT));
        context.setVariable("timeSlot", appointment.timeSlot().label());
        context.setVariable("truckType", appointment.truckType().name());
        context.setVariable("palletizationType", appointment.palletizationType().name());
        context.setVariable("palletQuantity", appointment.palletQuantity());
        context.setVariable("effectivePallets", appointment.effectivePallets());
        context.setVariable("warnings", warnings);
        return templateEngine.process("email/appointment-created", context);
    }
}
