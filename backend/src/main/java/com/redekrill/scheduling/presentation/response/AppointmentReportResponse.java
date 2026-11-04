/*
 * O pacote do relatório: consolidados do período, contagem filtrada e a página de
 * detalhes.
 *
 * Os campos se dividem em duas origens que não devem ser confundidas: totalAppointments,
 * totalPallets, totalEffectivePallets e totalCanceled são do período inteiro, sem filtro
 * de status; matchingAppointments, page, size e totalPages dizem respeito só ao que bate
 * com o status pedido. Uma tela que somar os itens da página no lugar de
 * matchingAppointments vai mostrar um número diferente a cada clique no paginador.
 *
 * totalPallets é o declarado; totalEffectivePallets é o ponderado pelo multiplicador de
 * paletização — a diferença entre os dois é o argumento para a faixa estar ocupada.
 *
 * A lista de agendamentos é copiada para imutável, igual às outras respostas compostas.
 */
package com.redekrill.scheduling.presentation.response;

import java.time.LocalDate;
import java.util.List;

public record AppointmentReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        int totalAppointments,
        long totalPallets,
        long totalEffectivePallets,
        int totalCanceled,
        int matchingAppointments,
        int page,
        int size,
        int totalPages,
        List<AppointmentResponse> appointments
) {

    public AppointmentReportResponse {
        appointments = List.copyOf(appointments);
    }
}
