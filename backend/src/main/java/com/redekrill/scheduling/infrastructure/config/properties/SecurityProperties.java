package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        boolean trustForwardedHeaders,
        @Min(1) int loginMaxAttemptsPerAccount,
        @Min(1) int loginMaxAttemptsPerIp,
        @NotNull Duration loginWindow,
        @NotNull Duration loginLockout
) {
}
