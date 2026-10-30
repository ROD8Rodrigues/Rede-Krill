package com.redekrill.scheduling.application.dto.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;

public record LoginResult(
        String token,
        String tokenType,
        long expiresIn,
        UserResult user
) {
}
