package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        boolean mustChangePassword,
        Instant createdAt
) {
}
