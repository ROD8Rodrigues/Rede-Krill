package com.redekrill.scheduling.application.dto.error;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de erro compartilhado entre a camada de aplicação/apresentação e os
 * manipuladores de segurança. Mantido sem dependências de framework.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        String code
) {

    public ApiError(Instant timestamp, int status, String error, String message,
                    String path, Map<String, String> fieldErrors) {
        this(timestamp, status, error, message, path, fieldErrors, null);
    }
}
