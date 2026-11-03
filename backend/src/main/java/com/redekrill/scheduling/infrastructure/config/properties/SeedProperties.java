package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        String adminPassword,
        boolean requirePasswordChange
) {
}
