package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;

public record CreateAppointmentCommand(
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        Long timeSlotId,
        Long truckTypeId,
        Long palletizationTypeId,
        int palletQuantity,
        String actorEmail
) {
}
