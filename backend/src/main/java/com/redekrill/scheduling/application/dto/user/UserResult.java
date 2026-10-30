package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;

import java.time.Instant;

public record UserResult(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        Instant createdAt
) {

    public static UserResult from(User user) {
        return new UserResult(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.active(),
                user.createdAt()
        );
    }
}
