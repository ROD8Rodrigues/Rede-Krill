package com.redekrill.scheduling.presentation.response;

import java.util.List;

public record AppointmentCommandResponse(
        AppointmentResponse appointment,
        List<String> warnings,
        CapacityResponse capacity,
        boolean emailQueued,
        boolean emailSent
) {

    public AppointmentCommandResponse {
        warnings = List.copyOf(warnings);
    }
}
