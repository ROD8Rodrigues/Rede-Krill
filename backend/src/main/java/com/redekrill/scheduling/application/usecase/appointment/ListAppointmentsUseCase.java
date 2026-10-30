package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

import java.time.LocalDate;

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
