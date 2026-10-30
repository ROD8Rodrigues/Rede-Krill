package com.redekrill.scheduling.presentation.response;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
