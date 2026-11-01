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

    static String requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new DomainException(message);
        }
        return value;
    }

    static String requireEmail(String value, String message) {
        String email = requireText(value, message).toLowerCase(java.util.Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new DomainException("E-mail inválido: " + email);
        }
        return email;
    }

    static String optionalText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new DomainException(message);
        }
        return text;
    }

    static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new DomainException(message);
        }
        return value;
    }

    static long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new DomainException(message);
        }
        return value;
    }
}
