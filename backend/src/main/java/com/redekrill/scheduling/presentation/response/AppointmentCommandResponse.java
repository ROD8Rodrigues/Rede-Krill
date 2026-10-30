package com.redekrill.scheduling.presentation.response;

import java.util.List;

public record AppointmentCommandResponse(
        AppointmentResponse appointment,
        List<String> warnings,
        CapacityResponse capacity
) {

    public AppointmentCommandResponse {
        warnings = List.copyOf(warnings);
    }
}
