/*
 * A visão de um agendamento para fora da camada de aplicação.
 *
 * O agregado Appointment não vai direto para o JSON por dois motivos. O primeiro
 * é que ele traz objetos inteiros (TimeSlot, TruckType, PalletizationType, User)
 * que a tela só precisa ver por dentro parcialmente. O segundo é que ele guarda o
 * passwordHash de quem criou — um tipo próprio de resposta garante que nenhuma
 * senha escapa por um toString esquecido.
 *
 * Os dois pares de números merecem atenção: palletQuantity e dailyPallets carregam
 * o mesmo valor, assim como effectivePallets e slotPallets. O primeiro par é o
 * nome do cadastro, o segundo é o nome que a política de capacidade usa. Isso está
 * aqui para que a resposta continue igual ao contrato publicado; qualquer
 * enxugamento precisa de uma decisão de API, não de gosto.
 *
 * Os dois records aninhados (TruckTypeSummaryResult, UserSummaryResult) são o
 * jeitinho para levar só id e nome de quem participa, sem copiar a entidade de
 * usuário inteira para a resposta.
 */
package com.redekrill.scheduling.application.dto.appointment;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.User;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Projeção imutável do agendamento para os casos de uso de leitura e para a
 * resposta HTTP. O método from é a única porta de entrada.
 */
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
