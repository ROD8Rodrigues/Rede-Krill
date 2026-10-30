package com.redekrill.scheduling.domain.exception;

public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
