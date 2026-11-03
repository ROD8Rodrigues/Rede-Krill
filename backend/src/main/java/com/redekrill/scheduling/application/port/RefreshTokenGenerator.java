package com.redekrill.scheduling.application.port;

public interface RefreshTokenGenerator {

    String generate();

    String hash(String rawToken);
}
