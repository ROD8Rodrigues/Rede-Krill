package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;

public record UpdateUserCommand(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        String password,
        String actorEmail
) {
}
