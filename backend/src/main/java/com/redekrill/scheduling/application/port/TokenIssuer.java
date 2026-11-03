package com.redekrill.scheduling.application.port;

import com.redekrill.scheduling.domain.model.User;

public interface TokenIssuer {

    IssuedToken issue(User user, Long sessionId);

    default IssuedToken issue(User user) {
        return issue(user, null);
    }
}
