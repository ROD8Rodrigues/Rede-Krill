/*
 * O relatório do período: a faixa de números em cima e a página de detalhes embaixo.
 *
 * Diferente da listagem comum, as duas datas são obrigatórias — relatório sem
 * período não tem como calcular contagem, soma e cancelados. A listagem completa
 * sentinelas; aqui a exigência é explicitamente recusada com DomainException.
 *
 * Os dois números de pallets que aparecem juntos não são alternativa um do outro: um
 * soma a quantidade declarada e o outro aplica o multiplicador da paletização. É a
 * diferença entre o que entrou no pedido e o que pesou na faixa.
 *
 * countByPeriod, sumPalletsByPeriod e countCanceledByPeriod olham o período inteiro,
 * enquanto totalElements olha só o que a página filtrada por status representa. As
 * contagens vêm long do banco e encolhem para int com Math.toIntExact — estourar seria um
 * problema de verdade, não algo a silenciar.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

import java.time.LocalDate;

/**
 * Consolidado e página de detalhes de um período, com filtro de status.
 */
public class GetAppointmentReportUseCase {

    private final AppointmentRepository appointmentRepository;

    public GetAppointmentReportUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentReportResult execute(LocalDate startDate, LocalDate endDate,
                                           AppointmentStatus status, int page, int size) {
        if (startDate == null || endDate == null) {
            throw new DomainException("Data inicial e data final são obrigatórias.");
        }
        if (startDate.isAfter(endDate)) {
            throw new DomainException("Data inicial não pode ser posterior à data final.");
        }

        int safePage = Pagination.page(page);
        int safeSize = Pagination.size(size);
        PageResult<Appointment> result = appointmentRepository.searchReport(
                startDate, endDate, status, safePage, safeSize);

        return new AppointmentReportResult(
                startDate,
                endDate,
                Math.toIntExact(appointmentRepository.countByPeriod(startDate, endDate)),
                appointmentRepository.sumPalletsByPeriod(startDate, endDate),
                appointmentRepository.sumEffectivePalletsByPeriod(startDate, endDate),
                Math.toIntExact(appointmentRepository.countCanceledByPeriod(startDate, endDate)),
                Math.toIntExact(result.totalElements()),
                result.page(),
                result.size(),
                result.totalPages(),
                result.content().stream().map(AppointmentResult::from).toList()
        );
    }
}
