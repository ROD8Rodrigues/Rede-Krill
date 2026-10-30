package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

public record User(
        Long id,
        String name,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public User {
        name = DomainValidation.requireText(name, "Nome do usuário é obrigatório.");
        email = DomainValidation.requireEmail(email, "E-mail do usuário é obrigatório.");
        passwordHash = DomainValidation.requireText(passwordHash, "Senha do usuário é obrigatória.");
        Objects.requireNonNull(role, "Perfil do usuário é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    public static User create(String name, String email, String passwordHash, Role role, Instant now) {
        return new User(null, name, email, passwordHash, role, true, now, now);
    }

    public User withId(Long newId) {
        return new User(newId, name, email, passwordHash, role, active, createdAt, updatedAt);
    }

    public User update(String newName, String newEmail, Role newRole, boolean newActive,
                       String newPasswordHash, Instant now) {
        return new User(id, newName, newEmail, newPasswordHash, newRole, newActive, createdAt, now);
    }
}
