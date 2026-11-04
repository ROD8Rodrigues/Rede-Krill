/*
 * A tradução entre o resultado da aplicação e o JSON que o cliente recebe.
 *
 * Métodos estáticos, privados de instância, sem estado. Cada um percorre os campos do
 * Result e monta o record de response correspondente, e é só isso. A alternativa —
 * Jackson anotado em cima do model de domínio — colocaria o agregado no lugar onde a
 * API é descrita, e qualquer campo novo do domínio vazaria para o cliente sem que
 * alguém tivesse decidido aquilo.
 *
 * Dois detalhes que são comportamento de contrato, não estilo:
 * - Hora da faixa sai formatada como HH:mm, em vez do ISO com segundos que LocalTime
 *   produziria sozinho. O formulário mostra "08:00", e formatar no mapper mantém o
 *   domínio sem máscara de exibição.
 * - Quem cancelou e quem criou viram resumo (id, nome, e-mail) ou nulo, nunca o objeto
 *   inteiro com papel, flag de senha e datas de conta.
 *
 * Quando um Result novo aparecer, o mapper é o primeiro lugar onde a forma da resposta é
 * decidida — e onde dá para ver, numa linha, o que o cliente vai poder ler.
 */
package com.redekrill.scheduling.presentation.mapper;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityOverviewResult;
import com.redekrill.scheduling.application.dto.appointment.CapacityResult;
import com.redekrill.scheduling.application.dto.appointment.SlotCapacityResult;
import com.redekrill.scheduling.application.dto.audit.AuditLogResult;
import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.presentation.response.AppointmentCommandResponse;
import com.redekrill.scheduling.presentation.response.AppointmentReportResponse;
import com.redekrill.scheduling.presentation.response.AppointmentResponse;
import com.redekrill.scheduling.presentation.response.AuditLogResponse;
import com.redekrill.scheduling.presentation.response.CapacityOverviewResponse;
import com.redekrill.scheduling.presentation.response.CapacityResponse;
import com.redekrill.scheduling.presentation.response.PalletizationTypeResponse;
import com.redekrill.scheduling.presentation.response.SlotCapacityResponse;
import com.redekrill.scheduling.presentation.response.TimeSlotResponse;
import com.redekrill.scheduling.presentation.response.TruckTypeResponse;
import com.redekrill.scheduling.presentation.response.UserResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Conversão unidirecional de DTOs de aplicação para records de resposta HTTP. Não faz
 * validação, não consulta nada e não conhece Servlet nem Jackson.
 */
public final class WebMapper {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private WebMapper() {
    }

    public static UserResponse toResponse(UserResult result) {
        return new UserResponse(
                result.id(),
                result.name(),
                result.email(),
                result.role(),
                result.active(),
                result.mustChangePassword(),
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
                result.orderId(),
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
                result.dailyPallets(),
                result.slotPallets(),
                result.notes(),
                result.status(),
                toResponse(result.canceledBy()),
                result.canceledAt(),
                toResponse(result.createdBy()),
                result.createdAt(),
                result.updatedAt()
        );
    }

    private static AppointmentResponse.UserSummaryResponse toResponse(
            AppointmentResult.UserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new AppointmentResponse.UserSummaryResponse(
                result.id(),
                result.name(),
                result.email()
        );
    }

    public static CapacityResponse toResponse(CapacityResult result) {
        return new CapacityResponse(
                result.dailyPallets(),
                result.slotPallets(),
                result.dailyLimit(),
                result.slotLimit(),
                result.dailyOccupancyPercentage(),
                result.slotOccupancyPercentage()
        );
    }

    public static SlotCapacityResponse toResponse(SlotCapacityResult result) {
        return new SlotCapacityResponse(
                toResponse(result.timeSlot()),
                result.pallets(),
                result.limit(),
                result.occupancyPercentage()
        );
    }

    public static CapacityOverviewResponse toResponse(CapacityOverviewResult result) {
        return new CapacityOverviewResponse(
                result.date(),
                result.dailyPallets(),
                result.dailyLimit(),
                result.dailyOccupancyPercentage(),
                result.slots().stream().map(WebMapper::toResponse).toList()
        );
    }

    public static AppointmentCommandResponse toResponse(AppointmentCommandResult result) {
        return new AppointmentCommandResponse(
                toResponse(result.appointment()),
                result.warnings(),
                toResponse(result.capacity()),
                result.emailQueued(),
                result.emailSent()
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
                result.totalPallets(),
                result.totalEffectivePallets(),
                result.totalCanceled(),
                result.matchingAppointments(),
                result.page(),
                result.size(),
                result.totalPages(),
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
