package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.policy.PasswordPolicy;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    private static final String NEW_PASSWORD = "SenhaNova@2026abc";
    private static final String CURRENT_HASH = "hashed-password";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private AuthSessionService authSessionService;
    @Mock
    private AuditService auditService;

    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangePasswordUseCase(
                userRepository,
                passwordHasher,
                authSessionService,
                auditService,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC),
                new PasswordPolicy()
        );
    }

    @Test
    void changesPasswordAndRevokesEverySession() {
        givenUser(TestFixtures.user());
        when(passwordHasher.matches("senha-atual", CURRENT_HASH)).thenReturn(true);
        when(passwordHasher.matches(NEW_PASSWORD, CURRENT_HASH)).thenReturn(false);
        when(passwordHasher.hash(NEW_PASSWORD)).thenReturn("new-hash");

        useCase.execute("admin@redekrill.com", "senha-atual", NEW_PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().passwordHash()).isEqualTo("new-hash");
        assertThat(captor.getValue().mustChangePassword()).isFalse();
        verify(auditService).record(eq("admin@redekrill.com"), eq(AuditAction.PASSWORD_CHANGE),
                eq("User"), eq(1L), contains("alterada pelo próprio"));
        verify(authSessionService).revokeAllForUser(1L);
    }

    @Test
    void keepsAuditOfRejectedAttemptAfterTransactionRollsBack() {
        // Com o runner que propaga a exceção, a auditoria da recusa precisa sobreviver:
        // ela é gravada fora da transação que falhou.
        givenUser(TestFixtures.user());
        when(passwordHasher.matches("senha-errada", CURRENT_HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-errada", NEW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Senha atual inválida.");

        verify(auditService).record(eq("admin@redekrill.com"), eq(AuditAction.PASSWORD_CHANGE),
                eq("User"), eq(1L), contains("recusada"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsPasswordIdenticalToCurrent() {
        givenUser(TestFixtures.user());
        when(passwordHasher.matches("senha-atual", CURRENT_HASH)).thenReturn(true);
        when(passwordHasher.matches(NEW_PASSWORD, CURRENT_HASH)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual", NEW_PASSWORD))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha deve ser diferente da senha atual.");
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnknownOrInactiveActor() {
        when(userRepository.findByEmail("admin@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual", NEW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não encontrado ou inativo.");
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnsignedRequest() {
        assertThatThrownBy(() -> useCase.execute(" ", "senha-atual", NEW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não identificado.");
    }

    @Test
    void rejectsBlankCurrentPassword() {
        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", " ", NEW_PASSWORD))
                .isInstanceOf(DomainException.class)
                .hasMessage("Senha atual é obrigatória.");
    }

    @Test
    void validatesNewPasswordPolicy() {
        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual", "curta1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha deve ter pelo menos 12 caracteres.");
        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual", "abcdefghijkl"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha deve conter letras e números.");
        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual",
                        "a".repeat(73)))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha deve ter no máximo 72 caracteres.");
        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com", "senha-atual", "SENHA1234567"))
                .isInstanceOf(DomainException.class)
                .hasMessage("A nova senha é conhecida ou insegura. Escolha outra.");
    }

    private void givenUser(User user) {
        when(userRepository.findByEmail("admin@redekrill.com")).thenReturn(Optional.of(user));
    }
}
