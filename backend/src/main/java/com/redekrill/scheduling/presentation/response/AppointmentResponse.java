package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlotResponse timeSlot,
        TruckTypeSummaryResponse truckType,
        PalletizationTypeResponse palletizationType,
        int palletQuantity,
        int effectivePallets,
        int dailyPallets,
        int slotPallets,
        String notes,
        AppointmentStatus status,
        UserSummaryResponse canceledBy,
        Instant canceledAt,
        UserSummaryResponse createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public record TruckTypeSummaryResponse(Long id, String name) {
    }

    public record UserSummaryResponse(Long id, String name, String email) {
    }
}
