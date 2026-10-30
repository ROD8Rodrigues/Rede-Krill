package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

import java.time.LocalDate;
import java.util.List;

public class GetAppointmentReportUseCase {

    private final AppointmentRepository appointmentRepository;

    public GetAppointmentReportUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentReportResult execute(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new DomainException("Data inicial e data final são obrigatórias.");
        }
        if (startDate.isAfter(endDate)) {
            throw new DomainException("Data inicial não pode ser posterior à data final.");
        }

        List<Appointment> appointments = appointmentRepository.findByPeriod(startDate, endDate);
        long totalEffectivePallets = appointments.stream()
                .mapToLong(Appointment::effectivePallets)
                .sum();

        return new AppointmentReportResult(
                startDate,
                endDate,
                appointments.size(),
                totalEffectivePallets,
                appointments.stream().map(AppointmentResult::from).toList()
        );
    }
}
