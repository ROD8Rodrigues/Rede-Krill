package com.redekrill.scheduling.application.port;

public record IssuedToken(String value, long expiresInSeconds) {
}
