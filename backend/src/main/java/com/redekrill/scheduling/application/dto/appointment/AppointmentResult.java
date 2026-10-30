package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.model.Appointment;

import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResult(
        Long id,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlotResult timeSlot,
        TruckTypeSummaryResult truckType,
        PalletizationTypeResult palletizationType,
        int palletQuantity,
        int effectivePallets,
        UserSummaryResult createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static AppointmentResult from(Appointment appointment) {
        return new AppointmentResult(
                appointment.id(),
                appointment.supplierName(),
                appointment.supplierEmail(),
                appointment.truckPlate(),
                appointment.date(),
                TimeSlotResult.from(appointment.timeSlot()),
                new TruckTypeSummaryResult(appointment.truckType().id(), appointment.truckType().name()),
                PalletizationTypeResult.from(appointment.palletizationType()),
                appointment.palletQuantity(),
                appointment.effectivePallets(),
                new UserSummaryResult(
                        appointment.createdBy().id(),
                        appointment.createdBy().name(),
                        appointment.createdBy().email()
                ),
                appointment.createdAt(),
                appointment.updatedAt()
        );
    }

    public record TruckTypeSummaryResult(Long id, String name) {
    }

    public record UserSummaryResult(Long id, String name, String email) {
    }
}
