package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 8) String adminPassword
) {
}
