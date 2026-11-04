/*
 * A resposta de criar ou editar um agendamento: registro, avisos, capacidade e e-mail.
 *
 * As duas flags de e-mail convivem porque o envio não acontece dentro da request:
 * emailQueued diz que a confirmação foi para a fila do outbox, e emailSent diz que o SMTP
 * aceitou a mensagem — no instante da resposta, a segunda é falsa, e o cliente que mostrar
 * "e-mail enviado" com base nela estará mentindo antes da hora.
 *
 * warnings traz o que a avaliação de capacidade produziu quando o agendamento entrou
 * (dia ou faixa acima do limite), e a lista é copiada para imutável no construtor
 * compacto: quem recebe a resposta não pode emendar o aviso que o domínio deu.
 *
 * capacity é o retrato depois da operação, não antes — é o que permite à tela atualizar o
 * medidor sem nova consulta.
 */
package com.redekrill.scheduling.presentation.response;

import java.util.List;

public record AppointmentCommandResponse(
        AppointmentResponse appointment,
        List<String> warnings,
        CapacityResponse capacity,
        boolean emailQueued,
        boolean emailSent
) {

    public AppointmentCommandResponse {
        warnings = List.copyOf(warnings);
    }
}
