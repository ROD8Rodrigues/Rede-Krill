package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @NotBlank String accessCookieName,
        @NotBlank String refreshCookieName,
        boolean cookieSecure,
        @NotBlank String cookieSameSite,
        String cookieDomain,
        boolean exposeTokenInResponse,
        @NotNull Duration refreshExpiration
) {
}
