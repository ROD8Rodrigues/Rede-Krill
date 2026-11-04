/*
 * O login, na ordem que a segurança pede.
 *
 * Primeiro a trava, depois a conta, por último a senha. Nessa ordem, um usuário
 * bloqueado nem chega à comparação de hash — e o custo de BCrypt é justamente o que
 * se quer evitar quando alguém martela a porta.
 *
 * As duas recusas de credencial devolvem exatamente a mesma frase. Diferenciar
 * "e-mail não existe" de "senha errada" é entregar ao atacante um catálogo de
 * contas válidas. O que muda fica no log de auditoria, que ninguém vê: tentativa com
 * usuário inexistente, tentativa com senha inválida e bloqueio por excesso têm ações
 * distintas.
 *
 * Toda recusa conta como falha, para a conta e para o IP; o sucesso zera o contador
 * da conta, mas não o do IP — o endereço continua devendo as tentativas que fez.
 * O e-mail é normalizado para minúsculas antes de tudo, porque a trava precisa bater
 * com a chave usada na verificação.
 *
 * A sessão é criada por AuthSessionService, não aqui: é ele que conhece hash de
 * refresh, expiração e rotação.
 */
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

/**
 * Autenticação com contagem de tentativas e criação de sessão.
 */
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
