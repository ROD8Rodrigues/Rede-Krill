package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record Appointment(
        Long id,
        Long orderId,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlot timeSlot,
        TruckType truckType,
        PalletizationType palletizationType,
        int palletQuantity,
        int effectivePallets,
        String notes,
        AppointmentStatus status,
        User canceledBy,
        Instant canceledAt,
        User createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    private static final int SUPPLIER_NAME_MAX_LENGTH = 80;
    private static final int SUPPLIER_EMAIL_MAX_LENGTH = 80;
    private static final int NOTES_MAX_LENGTH = 500;

    public Appointment {
        orderId = DomainValidation.requirePositive(
                orderId, "ID Pedido é obrigatório e deve ser maior que zero.");
        supplierName = DomainValidation.requireText(
                supplierName, "Nome do fornecedor é obrigatório.");
        supplierName = DomainValidation.requireMaxLength(
                supplierName, SUPPLIER_NAME_MAX_LENGTH,
                "Nome do fornecedor deve ter no máximo 80 caracteres.");
        supplierEmail = DomainValidation.requireEmail(
                supplierEmail, "E-mail do fornecedor é obrigatório.");
        supplierEmail = DomainValidation.requireMaxLength(
                supplierEmail, SUPPLIER_EMAIL_MAX_LENGTH,
                "E-mail do fornecedor deve ter no máximo 80 caracteres.");
        truckPlate = DomainValidation.requireText(
                truckPlate, "Placa do caminhão é obrigatória.").toUpperCase(Locale.ROOT);
        Objects.requireNonNull(date, "Data do agendamento é obrigatória.");
        Objects.requireNonNull(timeSlot, "Faixa horária é obrigatória.");
        Objects.requireNonNull(truckType, "Tipo de caminhão é obrigatório.");
        Objects.requireNonNull(palletizationType, "Tipo de paletização é obrigatório.");
        DomainValidation.requirePositive(
                palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        DomainValidation.requirePositive(
                effectivePallets, "Quantidade efetiva de pallets deve ser maior que zero.");
        notes = DomainValidation.optionalText(
                notes, NOTES_MAX_LENGTH, "Observação deve ter no máximo 500 caracteres.");
        Objects.requireNonNull(status, "Status do agendamento é obrigatório.");
        if (status == AppointmentStatus.CANCELED) {
            Objects.requireNonNull(canceledBy, "Usuário do cancelamento é obrigatório.");
            Objects.requireNonNull(canceledAt, "Data do cancelamento é obrigatória.");
        } else if (canceledBy != null || canceledAt != null) {
            throw new DomainException("Agendamento ativo não pode ter dados de cancelamento.");
        }
        Objects.requireNonNull(createdBy, "Usuário criador é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    public static Appointment create(
            Long orderId,
            String supplierName,
            String supplierEmail,
            String truckPlate,
            LocalDate date,
            TimeSlot timeSlot,
            TruckType truckType,
            PalletizationType palletizationType,
            int palletQuantity,
            String notes,
            User createdBy,
            Instant now
    ) {
        int effectivePallets = calculateEffectivePallets(palletQuantity, palletizationType);
        return new Appointment(
                null,
                orderId,
                supplierName,
                supplierEmail,
                truckPlate,
                date,
                timeSlot,
                truckType,
                palletizationType,
                palletQuantity,
                effectivePallets,
                notes,
                AppointmentStatus.ACTIVE,
                null,
                null,
                createdBy,
                now,
                now
        );
    }

    public Appointment withId(Long newId) {
        return new Appointment(
                newId, orderId, supplierName, supplierEmail, truckPlate, date, timeSlot,
                truckType, palletizationType, palletQuantity, effectivePallets, notes, status,
                canceledBy, canceledAt, createdBy, createdAt, updatedAt
        );
    }

    public Appointment update(
            Long newOrderId,
            String newSupplierName,
            String newSupplierEmail,
            String newTruckPlate,
            LocalDate newDate,
            TimeSlot newTimeSlot,
            TruckType newTruckType,
            PalletizationType newPalletizationType,
            int newPalletQuantity,
            String newNotes,
            Instant now
    ) {
        if (status == AppointmentStatus.CANCELED) {
            throw new ConflictException("Agendamento cancelado não pode ser alterado.");
        }
        int effectivePallets = calculateEffectivePallets(newPalletQuantity, newPalletizationType);
        return new Appointment(
                id,
                newOrderId,
                newSupplierName,
                newSupplierEmail,
                newTruckPlate,
                newDate,
                newTimeSlot,
                newTruckType,
                newPalletizationType,
                newPalletQuantity,
                effectivePallets,
                newNotes,
                status,
                canceledBy,
                canceledAt,
                createdBy,
                createdAt,
                now
        );
    }

    public Appointment cancel(User actor, Instant now) {
        if (status == AppointmentStatus.CANCELED) {
            throw new ConflictException("Agendamento já está cancelado.");
        }
        Objects.requireNonNull(actor, "Usuário do cancelamento é obrigatório.");
        Objects.requireNonNull(now, "Data do cancelamento é obrigatória.");
        return new Appointment(
                id, orderId, supplierName, supplierEmail, truckPlate, date, timeSlot,
                truckType, palletizationType, palletQuantity, effectivePallets, notes,
                AppointmentStatus.CANCELED, actor, now, createdBy, createdAt, now
        );
    }

    /**
     * Quantidade física declarada, usada no limite diário.
     */
    public int dailyPallets() {
        return palletQuantity;
    }

    /**
     * Quantidade considerada na faixa horária. Para carga não paletizada,
     * corresponde ao dobro da quantidade declarada.
     */
    public int slotPallets() {
        return effectivePallets;
    }

    private static int calculateEffectivePallets(int palletQuantity, PalletizationType palletizationType) {
        DomainValidation.requirePositive(palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        return Math.multiplyExact(palletQuantity, palletizationType.capacityMultiplier());
    }
}
