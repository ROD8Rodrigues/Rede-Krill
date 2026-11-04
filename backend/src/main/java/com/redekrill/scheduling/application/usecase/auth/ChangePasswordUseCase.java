/*
 * Troca de senha pelo próprio usuário, com o detalhe transacional que importa.
 *
 * A política é validada antes de abrir transação: "senha nova fraca" é erro de
 * entrada, não de estado. Dentro dela, a senha atual é conferida contra o hash.
 *
 * Quando a atual não bate, o caso de uso não lança ali mesmo. Ele devolve o usuário
 * para fora da transação e só então registra a auditoria e recusa. Fazer as duas
 * coisas dentro do mesmo escopo é o jeito de perder o indício: o rollback da exceção
 * apagaria justamente o log que documenta a tentativa.
 *
 * Dando certo, a senha nova tem de ser diferente da atual — trocar para a mesma é
 * fingir a troca sem ganhar nada. Depois de gravar, todas as sessões do usuário
 * caem: quem mudou a senha tem de reentrar com ela, e os tokens velhos perdem o
 * direito de existir.
 */
package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Alteração da própria senha, com revogação de sessões.
 */
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthSessionService authSessionService;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final PasswordPolicy passwordPolicy;

    public ChangePasswordUseCase(UserRepository userRepository,
                                 PasswordHasher passwordHasher,
                                 AuthSessionService authSessionService,
                                 AuditService auditService,
                                 TransactionRunner transactionRunner,
                                 Clock clock,
                                 PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.authSessionService = authSessionService;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.passwordPolicy = passwordPolicy;
    }

    public void execute(String actorEmail, String currentPassword, String newPassword) {
        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AuthenticationFailedException("Usuário autenticado não identificado.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new DomainException("Senha atual é obrigatória.");
        }
        passwordPolicy.validate(newPassword, "A nova senha");

        User rejected = transactionRunner.execute(() -> {
            User user = userRepository.findByEmail(actorEmail)
                    .filter(User::active)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            "Usuário autenticado não encontrado ou inativo."));

            if (!passwordHasher.matches(currentPassword, user.passwordHash())) {
                // A recusa é devolvida para fora da transação: registrar a auditoria aqui
                // e lançar em seguida faria o próprio rollback apagar o indício da tentativa.
                return user;
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

        if (rejected != null) {
            auditService.record(rejected.email(), AuditAction.PASSWORD_CHANGE, "User", rejected.id(),
                    "Troca de senha recusada: senha atual inválida.");
            throw new AuthenticationFailedException("Senha atual inválida.");
        }
    }
}
