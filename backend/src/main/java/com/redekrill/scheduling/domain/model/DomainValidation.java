package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.util.regex.Pattern;

final class DomainValidation {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private DomainValidation() {
    }

    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }

    static String requireEmail(String value, String message) {
        String email = requireText(value, message).toLowerCase(java.util.Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("E-mail inválido: " + email);
        }
        return email;
    }

    static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new DomainException(message);
        }
        return value;
    }
}
