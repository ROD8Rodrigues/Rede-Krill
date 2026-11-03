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
        Instant updatedAt,
        boolean mustChangePassword,
        long version
) {

    public User {
        name = DomainValidation.requireText(name, "Nome do usuário é obrigatório.");
        email = DomainValidation.requireEmail(email, "E-mail do usuário é obrigatório.");
        passwordHash = DomainValidation.requireText(passwordHash, "Senha do usuário é obrigatória.");
        Objects.requireNonNull(role, "Perfil do usuário é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação é obrigatória.");
        Objects.requireNonNull(updatedAt, "Data de atualização é obrigatória.");
    }

    /**
     * Construtor de compatibilidade usado por fixtures e por código que não
     * precisa informar os campos de segurança introduzidos na etapa 2.
     */
    public User(Long id, String name, String email, String passwordHash, Role role,
                boolean active, Instant createdAt, Instant updatedAt) {
        this(id, name, email, passwordHash, role, active, createdAt, updatedAt, false, 0L);
    }

    public static User create(String name, String email, String passwordHash, Role role, Instant now) {
        return new User(null, name, email, passwordHash, role, true, now, now, false, 0L);
    }

    public static User createInitial(String name, String email, String passwordHash, Role role,
                                     boolean mustChangePassword, Instant now) {
        return new User(null, name, email, passwordHash, role, true, now, now,
                mustChangePassword, 0L);
    }

    public User withId(Long newId) {
        return new User(newId, name, email, passwordHash, role, active, createdAt, updatedAt,
                mustChangePassword, version);
    }

    public User update(String newName, String newEmail, Role newRole, boolean newActive,
                       String newPasswordHash, Instant now) {
        return new User(id, newName, newEmail, newPasswordHash, newRole, newActive, createdAt, now,
                mustChangePassword, version);
    }

    public User changePassword(String newPasswordHash, Instant now) {
        return new User(id, name, email, newPasswordHash, role, active, createdAt, now,
                false, version);
    }

    public User resetPassword(String newPasswordHash, boolean requireChange, Instant now) {
        return new User(id, name, email, newPasswordHash, role, active, createdAt, now,
                requireChange, version);
    }
}
