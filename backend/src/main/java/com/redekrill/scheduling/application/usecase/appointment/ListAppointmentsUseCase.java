/*
 * A listagem paginada que alimenta a tela de agendamentos.
 *
 * As duas sentinelas de data existem porque a consulta do repositório usa um
 * between: não há caminho "sem limite inferior" no JPQL. Quando o cliente não
 * informa um dos lados, este caso de uso completa com 1970-01-01 ou 9999-12-31, e a
 * faixa fica aberta na prática sem mentir para a query.
 *
 * A data inicial posterior à final é recusada antes de tocar o banco — uma
 * consulta impossível devolveria lista vazia e o usuário acharia que não há
 * agendamentos.
 *
 * O PageResult volta remontado porque o repositório devolve modelos Appointment e
 * a resposta precisa de DTOs; os metadados de página (número, tamanho, totais) são
 * copiados, não recalculados. A ordem vem da consulta: data,
 * horário da faixa e id, todos descendentes.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

import java.time.LocalDate;

/**
 * Busca paginada de agendamentos por período.
 */
public class ListAppointmentsUseCase {

    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final AppointmentRepository appointmentRepository;

    public ListAppointmentsUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public PageResult<AppointmentResult> execute(LocalDate startDate, LocalDate endDate,
                                                 int page, int size) {
        LocalDate start = startDate == null ? MIN_DATE : startDate;
        LocalDate end = endDate == null ? MAX_DATE : endDate;
        if (start.isAfter(end)) {
            throw new DomainException("Data inicial não pode ser posterior à data final.");
        }

        PageResult<Appointment> result = appointmentRepository.search(
                start, end, Pagination.page(page), Pagination.size(size));

        return PageResult.of(
                result.content().stream().map(AppointmentResult::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
