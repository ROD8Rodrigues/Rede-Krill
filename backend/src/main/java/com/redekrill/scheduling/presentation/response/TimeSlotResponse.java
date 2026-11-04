/*
 * A faixa horária como texto pronto para exibição.
 *
 * startTime e endTime são Strings, não LocalTime: WebMapper os formata em HH:mm, e isso
 * evita que o cliente veja "08:00:00" ou a hora com offset dependendo da configuração de
 * serialização. O contrato fica estável mesmo se o domínio mudar de tipo interno.
 *
 * label é a frase que a faixa mostra no select ("08:00 - 10:00"). Ele vem do domínio, o
 * que deixa a montagem do rótulo em um lugar só — inclusive no e-mail de confirmação, onde
 * a faixa também aparece.
 */
package com.redekrill.scheduling.presentation.response;

public record TimeSlotResponse(
        Long id,
        String startTime,
        String endTime,
        String label
) {
}
