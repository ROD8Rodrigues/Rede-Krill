package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class ChangePasswordUseCase {

    private static final int MIN_LENGTH = 12;
    private static final List<String> FORBIDDEN_PASSWORDS = List.of(
            "admin@12345", "redekrill", "password", "senha123456", "123456789012"
    );

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthSessionService authSessionService;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordHasher passwordHasher,
                                 AuthSessionService authSessionService,
                                 AuditService auditService,
                                 TransactionRunner transactionRunner,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.authSessionService = authSessionService;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public void execute(String actorEmail, String currentPassword, String newPassword) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new DomainException("Senha atual é obrigatória.");
        }
        validateNewPassword(newPassword);

        transactionRunner.execute(() -> {
            User user = userRepository.findByEmail(actorEmail)
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário autenticado não encontrado ou inativo."));

            if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
                auditService.record(user.email(), AuditAction.PASSWORD_CHANGE, "User", user.id(),
                        "Troca de senha recusada: senha atual inválida.");
                throw new AuthenticationFailedException("Senha atual inválida.");
            }
            if (passwordHasher.matches(newPassword, user.passwordHash())) {
                throw new DomainException("A nova senha deve ser diferente da senha atual.");
            }

            userRepository.save(user.changePassword(
                    passwordHasher.hash(newPassword), Instant.now(clock)));
            auditService.record(user.email(), AuditAction.PASSWORD_CHANGE, "User", user.id(),
                    "Senha alterada pelo próprio usuário.");
            authSessionService.revokeAllForUser(user.id());
            return null;
        });
    }

    private void validateNewPassword(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new DomainException(
                    "A nova senha deve ter pelo menos %d caracteres.".formatted(MIN_LENGTH));
        }
        if (password.length() > 72) {
            throw new DomainException("A nova senha deve ter no máximo 72 caracteres.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new DomainException("A nova senha deve conter letras e números.");
        }
        if (FORBIDDEN_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new DomainException("A nova senha é conhecida ou insegura. Escolha outra.");
        }
    }
}
