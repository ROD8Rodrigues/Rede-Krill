package com.redekrill.scheduling.application.dto.appointment;

import java.time.LocalDate;

public record UpdateAppointmentCommand(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        Long timeSlotId,
        Long truckTypeId,
        Long palletizationTypeId,
        int palletQuantity,
        String notes,
        String actorEmail
) {
}
