package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UpdateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

public class UpdateUserUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public UpdateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                             AuditService auditService, TransactionRunner transactionRunner,
                             Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    public UserResult execute(UpdateUserCommand command) {
        if (command == null) {
            throw new DomainException("Dados do usuário são obrigatórios.");
        }
        if (command.role() == null) {
            throw new DomainException("Perfil do usuário é obrigatório.");
        }

        User existing = userRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        validateSelfUpdate(existing, command);

        String email = normalizeEmail(command.email());
        if (userRepository.existsByEmailAndIdNot(email, existing.id())) {
            throw new ConflictException("Já existe um usuário com o e-mail informado.");
        }

        String passwordHash = existing.passwordHash();
        if (command.password() != null && !command.password().isBlank()) {
            if (command.password().length() < MIN_PASSWORD_LENGTH) {
                throw new DomainException("Senha deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
            }
            passwordHash = passwordHasher.hash(command.password());
        }

        User updated = existing.update(
                command.name(),
                email,
                command.role(),
                command.active(),
                passwordHash,
                Instant.now(clock)
        );

        User saved = transactionRunner.execute(() -> {
            User result = userRepository.save(updated);
            auditService.record(
                    command.actorEmail(),
                    AuditAction.UPDATE,
                    "User",
                    result.id(),
                    "Usuário atualizado: " + result.email()
            );
            return result;
        });

        return UserResult.from(saved);
    }

    private void validateSelfUpdate(User existing, UpdateUserCommand command) {
        boolean isSelf = command.actorEmail() != null
                && existing.email().equalsIgnoreCase(command.actorEmail().trim());

        if (!isSelf) {
            return;
        }
        if (!command.active()) {
            throw new ConflictException("Administrador não pode desativar a própria conta.");
        }
        if (command.role() != Role.ADMIN) {
            throw new ConflictException("Administrador não pode remover o próprio perfil ADMIN.");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("E-mail do usuário é obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
