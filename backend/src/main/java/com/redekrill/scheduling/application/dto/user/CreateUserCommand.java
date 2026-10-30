package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;

public record CreateUserCommand(
        String name,
        String email,
        String password,
        Role role,
        String actorEmail
) {
}
