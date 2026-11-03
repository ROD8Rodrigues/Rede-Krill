package com.redekrill.scheduling.presentation.response;

import java.time.LocalDate;
import java.util.List;

public record AppointmentReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        int totalAppointments,
        long totalPallets,
        long totalEffectivePallets,
        int page,
        int size,
        int totalPages,
        List<AppointmentResponse> appointments
) {

    public AppointmentReportResponse {
        appointments = List.copyOf(appointments);
    }
}
