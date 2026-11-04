/*
 * Edição de usuário, que é três operações diferentes dependendo do que veio.
 *
 * Edição de perfil é o caminho comum: nome, e-mail, papel e atividade. Se o campo de
 * senha vier preenchido, o caso de uso assume reset feito por ADMIN — valida a
 * política, troca o hash e acende a flag de troca obrigatória, porque a nova senha foi
 * escolhida por outra pessoa de novo. Vindo vazio, o hash existente é copiado tal e
 * qual; não existe "senha em branco significa senha nova".
 *
 * Há um guarda de autolesão: quem edita a própria conta não pode se desativar nem
 * tirar de si o papel ADMIN. As duas portas impedem o único roteiro em que o sistema
 * fica sem ninguém capaz de entrar para consertá-lo.
 *
 * Por fim, a sessão. Desativar ou trocar a senha derruba todas as sessões do usuário,
 * do lado de fora da transação de escrita — o logout forçado é consequência, não parte
 * da gravação do perfil.
 */
package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UpdateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TransactionRunner;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
import com.redekrill.scheduling.domain.repository.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Atualiza perfil e, quando enviada, redefine a senha de um usuário.
 */
public class UpdateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;
    private final TransactionRunner transactionRunner;
    private final AuthSessionService authSessionService;
    private final Clock clock;
    private final PasswordPolicy passwordPolicy;

    public UpdateUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                             AuditService auditService, TransactionRunner transactionRunner,
                             AuthSessionService authSessionService, Clock clock,
                             PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
        this.transactionRunner = transactionRunner;
        this.authSessionService = authSessionService;
        this.clock = clock;
        this.passwordPolicy = passwordPolicy;
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

        boolean passwordChanged = command.password() != null && !command.password().isBlank();
        if (passwordChanged) {
            passwordPolicy.validate(command.password(), "A senha");
        }
        Instant now = Instant.now(clock);
        String passwordHash = passwordChanged
                ? passwordHasher.hash(command.password())
                : existing.passwordHash();
        User profile = existing.update(
                command.name(),
                email,
                command.role(),
                command.active(),
                passwordHash,
                now
        );
        User updated = passwordChanged
                ? profile.resetPassword(passwordHash, true, now)
                : profile;

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

        if (!saved.active() || passwordChanged) {
            authSessionService.revokeAllForUser(saved.id());
        }
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
