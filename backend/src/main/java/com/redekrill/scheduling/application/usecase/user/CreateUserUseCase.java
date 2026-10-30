package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.CreateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

public class CreateUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                             AuditService auditService, TransactionRunner transactionRunner,
                             Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public UserResult execute(CreateUserCommand command) {
        if (command == null) {
            throw new DomainException("Dados do usuário são obrigatórios.");
        }
        if (command.role() == null) {
            throw new DomainException("Perfil do usuário é obrigatório.");
        }
        validatePassword(command.password());

        String email = normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário com o e-mail informado.");
        }

        User user = User.create(
                command.name(),
                email,
                passwordHasher.hash(command.password()),
                command.role(),
                Instant.now(clock)
        );

        User saved = transactionRunner.execute(() -> {
            User created = userRepository.save(user);
            auditService.record(
                    command.actorEmail(),
                    AuditAction.CREATE,
                    "User",
                    created.id(),
                    "Usuário criado: " + created.email()
            );
            return created;
        });

        return UserResult.from(saved);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new DomainException("Senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("E-mail do usuário é obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
