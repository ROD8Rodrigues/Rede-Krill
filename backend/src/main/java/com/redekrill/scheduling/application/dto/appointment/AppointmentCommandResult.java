package com.redekrill.scheduling.application.dto.appointment;

import java.util.List;

public record AppointmentCommandResult(
        AppointmentResult appointment,
        List<String> warnings,
        CapacityResult capacity
) {

    public AppointmentCommandResult {
        warnings = List.copyOf(warnings);
    }
}
