package com.redekrill.scheduling.application.dto.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;

public record SessionTokens(
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshExpiresIn,
        long sessionId,
        UserResult user
) {
}
