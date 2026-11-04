/*
 * O relatório do período, com os totais em cima e a página de registros embaixo.
 *
 * Um único objeto carrega as duas metades porque a tela precisa das duas juntas:
 * os cartões de resumo (total de agendamentos, pallets declarados, pallets
 * considerados, cancelados) são contados sobre todo o período, enquanto a listagem
 * é paginada. Separar em duas requisições deixaria o usuário ver um número de
 * outro período se os dados mudassem entre uma e outra.
 *
 * totalPallets soma a quantidade declarada; totalEffectivePallets soma a
 * considerada, que recebe o multiplicador da paletização. Os dois aparecem porque
 * medem coisas diferentes: o volume do dia e a ocupação das faixas.
 */
package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;
import java.util.List;

/**
 * Consolidado do período mais a página de agendamentos que o compõe.
 */
public record AppointmentReportResult(
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
        List<AppointmentResult> appointments
) {

    public AppointmentReportResult {
        appointments = List.copyOf(appointments);
    }
}
