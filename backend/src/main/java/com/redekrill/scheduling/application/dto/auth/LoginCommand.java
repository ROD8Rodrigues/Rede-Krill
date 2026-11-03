package com.redekrill.scheduling.application.dto.auth;

public record LoginCommand(String email, String password, String clientIp, String userAgent) {

    public LoginCommand(String email, String password) {
        this(email, password, null, null);
    }
}
