package com.redekrill.scheduling.presentation.response;

import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlotResponse timeSlot,
        TruckTypeSummaryResponse truckType,
        PalletizationTypeResponse palletizationType,
        int palletQuantity,
        int effectivePallets,
        UserSummaryResponse createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public record TruckTypeSummaryResponse(Long id, String name) {
    }

    public record UserSummaryResponse(Long id, String name, String email) {
    }
}
