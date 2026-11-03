package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;
import java.util.List;

public record AppointmentReportResult(
        LocalDate startDate,
        LocalDate endDate,
        int totalAppointments,
        long totalPallets,
        long totalEffectivePallets,
        int page,
        int size,
        int totalPages,
        List<AppointmentResult> appointments
) {

    public AppointmentReportResult {
        appointments = List.copyOf(appointments);
    }
}
