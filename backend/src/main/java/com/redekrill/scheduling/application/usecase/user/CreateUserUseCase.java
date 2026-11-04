/*
 * Cadastro de usuário feito por ADMIN, com a senha nascendo nas mãos de outra pessoa.
 *
 * A política de senha é checada antes de qualquer consulta ao banco, e o hash é
 * calculado antes da transação: cifra de BCrypt é trabalho de CPU, e segurando isso
 * dentro da transação se paga conexão aberta por tempo de computação.
 *
 * O usuário nasce com a flag de troca de senha obrigatória ligada, porque a senha que
 * entrou foi escolhida por outro. É isso que faz o filtro de troca obrigatória pegar o
 * primeiro acesso.
 *
 * O e-mail é aparado, passado para minúsculas e comparado assim com os registros existentes — a
 * chave de login é o texto normalizado, e contas "Ana@X" e "ana@x" são uma bagunça que não se
 * resolve depois.
 */
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
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Cria um usuário com senha inicial definida por outro.
 */
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final PasswordPolicy passwordPolicy;

    public CreateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                             AuditService auditService, TransactionRunner transactionRunner,
                             Clock clock, PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.passwordPolicy = passwordPolicy;
    }

    public UserResult execute(CreateUserCommand command) {
        if (command == null) {
            throw new DomainException("Dados do usuário são obrigatórios.");
        }
        if (command.role() == null) {
            throw new DomainException("Perfil do usuário é obrigatório.");
        }
        passwordPolicy.validate(command.password(), "A senha");

        String email = normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário com o e-mail informado.");
        }

        User user = User.createInitial(
                command.name(),
                email,
                passwordHasher.hash(command.password()),
                command.role(),
                true,
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

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("E-mail do usuário é obrigatório.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
