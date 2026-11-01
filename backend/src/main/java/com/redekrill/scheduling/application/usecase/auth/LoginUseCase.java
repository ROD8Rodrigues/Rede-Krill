package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.LoginResult;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.util.Locale;

public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final AuditService auditService;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                        TokenIssuer tokenIssuer, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.auditService = auditService;
    }

    public LoginResult execute(LoginCommand command) {
        if (command == null || command.email() == null || command.email().isBlank()
                || command.password() == null || command.password().isBlank()) {
            throw new DomainException("E-mail e senha são obrigatórios.");
        }

        String email = command.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .filter(User::active)
                .orElseThrow(() -> new AuthenticationFailedException("E-mail ou senha inválidos."));

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new AuthenticationFailedException("E-mail ou senha inválidos.");
        }

        IssuedToken token = tokenIssuer.issue(user);
        auditService.record(user.email(), AuditAction.LOGIN, "User", user.id(), "Login realizado.");
        return new LoginResult(token.value(), "Bearer", token.expiresInSeconds(), UserResult.from(user));
    }
}
