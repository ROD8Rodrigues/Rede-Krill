package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

import java.time.LocalDate;

public class GetAppointmentReportUseCase {

    private final AppointmentRepository appointmentRepository;

    public GetAppointmentReportUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentReportResult execute(LocalDate startDate, LocalDate endDate) {
        return execute(startDate, endDate, 0, 20);
    }

    public AppointmentReportResult execute(LocalDate startDate, LocalDate endDate,
                                           int page, int size) {
        if (startDate == null || endDate == null) {
            throw new DomainException("Data inicial e data final são obrigatórias.");
        }
        if (startDate.isAfter(endDate)) {
            throw new DomainException("Data inicial não pode ser posterior à data final.");
        }

        int safePage = Pagination.page(page);
        int safeSize = Pagination.size(size);
        PageResult<Appointment> result = appointmentRepository.searchReport(
                startDate, endDate, safePage, safeSize);

        return new AppointmentReportResult(
                startDate,
                endDate,
                Math.toIntExact(appointmentRepository.countByPeriod(startDate, endDate)),
                appointmentRepository.sumPalletsByPeriod(startDate, endDate),
                appointmentRepository.sumEffectivePalletsByPeriod(startDate, endDate),
                result.page(),
                result.size(),
                result.totalPages(),
                result.content().stream().map(AppointmentResult::from).toList()
        );
    }
}
