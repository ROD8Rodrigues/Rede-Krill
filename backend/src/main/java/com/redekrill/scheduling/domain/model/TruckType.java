package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TruckType(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public TruckType {
        name = DomainValidation.requireText(name, "Nome do tipo de caminhão é obrigatório.");
        description = description == null || description.isBlank() ? null : description.trim();
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    public static TruckType create(String name, String description, Instant now) {
        return new TruckType(null, name, description, now, now);
    }

    public TruckType withId(Long newId) {
        return new TruckType(newId, name, description, createdAt, updatedAt);
    }

    public TruckType update(String newName, String newDescription, Instant now) {
        return new TruckType(id, newName, newDescription, createdAt, now);
    }
}
