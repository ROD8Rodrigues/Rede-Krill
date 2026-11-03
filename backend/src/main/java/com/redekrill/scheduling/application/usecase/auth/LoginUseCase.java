package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.application.service.LoginAttemptService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.TooManyRequestsException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.util.Locale;
import java.util.Optional;

public class LoginUseCase {

    private static final String GENERIC_FAILURE = "E-mail ou senha inválidos.";
    private static final String ANONYMOUS_ACTOR = "anonimo@redekrill.local";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;
    private final AuthSessionService authSessionService;

    public LoginUseCase(UserRepository userRepository,
                        PasswordHasher passwordHasher,
                        AuditService auditService,
                        LoginAttemptService loginAttemptService,
                        AuthSessionService authSessionService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.loginAttemptService = loginAttemptService;
        this.authSessionService = authSessionService;
    }

    public SessionTokens execute(LoginCommand command) {
        if (command == null || command.email() == null || command.email().isBlank()
                || command.password() == null || command.password().isBlank()) {
            throw new DomainException("E-mail e senha são obrigatórios.");
        }

        String email = command.email().trim().toLowerCase(Locale.ROOT);
        try {
            loginAttemptService.assertAllowed(email, command.clientIp());
        } catch (TooManyRequestsException exception) {
            audit(email, AuditAction.LOGIN_BLOCKED,
                    "Login bloqueado temporariamente por excesso de tentativas.");
            throw exception;
        }

        Optional<User> found = userRepository.findByEmail(email).filter(User::active);
        if (found.isEmpty()) {
            loginAttemptService.recordFailure(email, command.clientIp());
            audit(email, AuditAction.LOGIN_FAILED, "Tentativa de login com usuário inexistente ou inativo.");
            throw new AuthenticationFailedException(GENERIC_FAILURE);
        }

        User user = found.get();
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            loginAttemptService.recordFailure(email, command.clientIp());
            audit(email, AuditAction.LOGIN_FAILED, "Tentativa de login com senha inválida.");
            throw new AuthenticationFailedException(GENERIC_FAILURE);
        }

        loginAttemptService.recordSuccess(email);
        SessionTokens tokens = authSessionService.start(
                user, command.clientIp(), command.userAgent());
        audit(user.email(), AuditAction.LOGIN, "Login realizado.");
        return tokens;
    }

    private void audit(String email, AuditAction action, String details) {
        String actor = email == null || email.isBlank() ? ANONYMOUS_ACTOR : email;
        auditService.record(actor, action, "User", null, details);
    }
}
