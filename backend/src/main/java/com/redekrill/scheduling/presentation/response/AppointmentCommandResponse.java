package com.redekrill.scheduling.presentation.response;

import java.util.List;

public record AppointmentCommandResponse(
        AppointmentResponse appointment,
        List<String> warnings,
        CapacityResponse capacity,
        Boolean emailSent
) {

    public AppointmentCommandResponse {
        warnings = List.copyOf(warnings);
    }
}
