package com.redekrill.scheduling.infrastructure.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.email.outbox")
public record EmailOutboxProperties(
        boolean enabled,
        @Min(1) int maxAttempts,
        @Min(1000) long pollIntervalMs,
        @Min(1) long initialBackoffSeconds,
        @Min(1) int batchSize
) {
}
