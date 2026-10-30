package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public record Appointment(
        Long id,
        String supplierName,
        String supplierEmail,
        String truckPlate,
        LocalDate date,
        TimeSlot timeSlot,
        TruckType truckType,
        PalletizationType palletizationType,
        int palletQuantity,
        int effectivePallets,
        User createdBy,
        Instant createdAt,
        Instant updatedAt
) {

    public Appointment {
        supplierName = DomainValidation.requireText(supplierName, "Nome do fornecedor é obrigatório.");
        supplierEmail = DomainValidation.requireEmail(supplierEmail, "E-mail do fornecedor é obrigatório.");
        truckPlate = DomainValidation.requireText(truckPlate, "Placa do caminhão é obrigatória.")
                .toUpperCase(Locale.ROOT);
        Objects.requireNonNull(date, "Data do agendamento é obrigatória.");
        Objects.requireNonNull(timeSlot, "Faixa horária é obrigatória.");
        Objects.requireNonNull(truckType, "Tipo de caminhão é obrigatório.");
        Objects.requireNonNull(palletizationType, "Tipo de paletização é obrigatório.");
        DomainValidation.requirePositive(palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        DomainValidation.requirePositive(effectivePallets, "Quantidade efetiva de pallets deve ser maior que zero.");
        Objects.requireNonNull(createdBy, "Usuário criador é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    public static Appointment create(
            String supplierName,
            String supplierEmail,
            String truckPlate,
            LocalDate date,
            TimeSlot timeSlot,
            TruckType truckType,
            PalletizationType palletizationType,
            int palletQuantity,
            User createdBy,
            Instant now
    ) {
        int effectivePallets = calculateEffectivePallets(palletQuantity, palletizationType);
        return new Appointment(
                null,
                supplierName,
                supplierEmail,
                truckPlate,
                date,
                timeSlot,
                truckType,
                palletizationType,
                palletQuantity,
                effectivePallets,
                createdBy,
                now,
                now
        );
    }

    public Appointment withId(Long newId) {
        return new Appointment(
                newId, supplierName, supplierEmail, truckPlate, date, timeSlot, truckType,
                palletizationType, palletQuantity, effectivePallets, createdBy, createdAt, updatedAt
        );
    }

    public Appointment update(
            String newSupplierName,
            String newSupplierEmail,
            String newTruckPlate,
            LocalDate newDate,
            TimeSlot newTimeSlot,
            TruckType newTruckType,
            PalletizationType newPalletizationType,
            int newPalletQuantity,
            Instant now
    ) {
        int effectivePallets = calculateEffectivePallets(newPalletQuantity, newPalletizationType);
        return new Appointment(
                id,
                newSupplierName,
                newSupplierEmail,
                newTruckPlate,
                newDate,
                newTimeSlot,
                newTruckType,
                newPalletizationType,
                newPalletQuantity,
                effectivePallets,
                createdBy,
                createdAt,
                now
        );
    }

    private static int calculateEffectivePallets(int palletQuantity, PalletizationType palletizationType) {
        DomainValidation.requirePositive(palletQuantity, "Quantidade de pallets deve ser maior que zero.");
        return Math.multiplyExact(palletQuantity, palletizationType.capacityMultiplier());
    }
}
