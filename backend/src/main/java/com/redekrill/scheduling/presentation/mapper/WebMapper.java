package com.redekrill.scheduling.presentation.mapper;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityResult;
import com.redekrill.scheduling.application.dto.audit.AuditLogResult;
import com.redekrill.scheduling.application.dto.auth.LoginResult;
import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.presentation.response.AppointmentCommandResponse;
import com.redekrill.scheduling.presentation.response.AppointmentReportResponse;
import com.redekrill.scheduling.presentation.response.AppointmentResponse;
import com.redekrill.scheduling.presentation.response.AuditLogResponse;
import com.redekrill.scheduling.presentation.response.CapacityResponse;
import com.redekrill.scheduling.presentation.response.LoginResponse;
import com.redekrill.scheduling.presentation.response.PalletizationTypeResponse;
import com.redekrill.scheduling.presentation.response.TimeSlotResponse;
import com.redekrill.scheduling.presentation.response.TruckTypeResponse;
import com.redekrill.scheduling.presentation.response.UserResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class WebMapper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private WebMapper() {
    }

    public static LoginResponse toResponse(LoginResult result) {
        return new LoginResponse(
                result.token(),
                result.tokenType(),
                result.expiresIn(),
                toResponse(result.user())
        );
    }

    public static UserResponse toResponse(UserResult result) {
        return new UserResponse(
                result.id(),
                result.name(),
                result.email(),
                result.role(),
                result.active(),
                result.createdAt()
        );
    }

    public static TruckTypeResponse toResponse(TruckTypeResult result) {
        return new TruckTypeResponse(
                result.id(),
                result.name(),
                result.description(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static TimeSlotResponse toResponse(TimeSlotResult result) {
        return new TimeSlotResponse(
                result.id(),
                result.startTime().format(TIME_FORMAT),
                result.endTime().format(TIME_FORMAT),
                result.label()
        );
    }

    public static PalletizationTypeResponse toResponse(PalletizationTypeResult result) {
        return new PalletizationTypeResponse(
                result.id(),
                result.code(),
                result.name(),
                result.capacityMultiplier()
        );
    }

    public static AppointmentResponse toResponse(AppointmentResult result) {
        return new AppointmentResponse(
                result.id(),
                result.supplierName(),
                result.supplierEmail(),
                result.truckPlate(),
                result.date(),
                toResponse(result.timeSlot()),
                new AppointmentResponse.TruckTypeSummaryResponse(
                        result.truckType().id(), result.truckType().name()),
                toResponse(result.palletizationType()),
                result.palletQuantity(),
                result.effectivePallets(),
                new AppointmentResponse.UserSummaryResponse(
                        result.createdBy().id(),
                        result.createdBy().name(),
                        result.createdBy().email()
                ),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static CapacityResponse toResponse(CapacityResult result) {
        return new CapacityResponse(
                result.dailyPallets(),
                result.slotPallets(),
                result.dailyLimit(),
                result.slotLimit()
        );
    }

    public static AppointmentCommandResponse toResponse(AppointmentCommandResult result) {
        return new AppointmentCommandResponse(
                toResponse(result.appointment()),
                result.warnings(),
                toResponse(result.capacity())
        );
    }

    public static AppointmentReportResponse toResponse(AppointmentReportResult result) {
        List<AppointmentResponse> appointments = result.appointments().stream()
                .map(WebMapper::toResponse)
                .toList();

        return new AppointmentReportResponse(
                result.startDate(),
                result.endDate(),
                result.totalAppointments(),
                result.totalEffectivePallets(),
                appointments
        );
    }

    public static AuditLogResponse toResponse(AuditLogResult result) {
        return new AuditLogResponse(
                result.id(),
                result.actorEmail(),
                result.action(),
                result.entityName(),
                result.entityId(),
                result.details(),
                result.createdAt()
        );
    }
}
