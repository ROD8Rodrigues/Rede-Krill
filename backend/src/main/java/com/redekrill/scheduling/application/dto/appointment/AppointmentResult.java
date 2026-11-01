package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.User;

import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResult(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlotResult timeSlot,
        TruckTypeSummaryResult truckType,
        PalletizationTypeResult palletizationType,
        int palletQuantity,
        int effectivePallets,
        int dailyPallets,
        int slotPallets,
        String notes,
        AppointmentStatus status,
        UserSummaryResult canceledBy,
        Instant canceledAt,
        UserSummaryResult createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public static AppointmentResult from(Appointment appointment) {
        return new AppointmentResult(
                appointment.id(),
                appointment.orderId(),
                appointment.supplierName(),
                appointment.supplierEmail(),
                appointment.truckPlate(),
                appointment.date(),
                TimeSlotResult.from(appointment.timeSlot()),
                new TruckTypeSummaryResult(appointment.truckType().id(), appointment.truckType().name()),
                PalletizationTypeResult.from(appointment.palletizationType()),
                appointment.palletQuantity(),
                appointment.effectivePallets(),
                appointment.dailyPallets(),
                appointment.slotPallets(),
                appointment.notes(),
                appointment.status(),
                summary(appointment.canceledBy()),
                appointment.canceledAt(),
                summary(appointment.createdBy()),
                appointment.createdAt(),
                appointment.updatedAt()
        );
    }

    private static UserSummaryResult summary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResult(user.id(), user.name(), user.email());
    }

    public record TruckTypeSummaryResult(Long id, String name) {
    }

    public record UserSummaryResult(Long id, String name, String email) {
    }
}
