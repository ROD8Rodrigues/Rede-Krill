package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UpdateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.ImmediateTransactionRunner;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UpdateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private AuditService auditService;

    private UpdateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserUseCase(
                userRepository, passwordHasher, auditService,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void updatesUserWithoutChangingPassword() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(TestFixtures.otherUser()));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, User.class));

        UserResult result = useCase.execute(new UpdateUserCommand(
                2L, "Novo Nome", "Novo@RedeKrill.com", Role.USER, false, null, "admin@redekrill.com"));

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(result.email()).isEqualTo("novo@redekrill.com");
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(result.active()).isFalse();
        verify(passwordHasher, never()).hash(any());
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.UPDATE),
                eq("User"),
                eq(2L),
                contains("novo@redekrill.com")
        );
    }

    @Test
    void rejectsSelfDeactivation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Administrador", "admin@redekrill.com", Role.ADMIN, false, null,
                "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Administrador não pode desativar a própria conta.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsSelfRemovalOfAdminRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Administrador", "admin@redekrill.com", Role.USER, true, null,
                "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Administrador não pode remover o próprio perfil ADMIN.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatesUserWithNewPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));
        when(passwordHasher.hash("NovaSenha@123")).thenReturn("novo-hash");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, User.class));

        UserResult result = useCase.execute(new UpdateUserCommand(
                1L, "Administrador", "admin@redekrill.com", Role.ADMIN, true,
                "NovaSenha@123", "admin@redekrill.com"));

        assertThat(result.email()).isEqualTo("admin@redekrill.com");
        verify(passwordHasher).hash("NovaSenha@123");
    }

    @Test
    void rejectsMissingCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Dados do usuário são obrigatórios.");
    }

    @Test
    void rejectsMissingRole() {
        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Nome", "admin@redekrill.com", null, true, null, "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("Perfil do usuário é obrigatório.");
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                99L, "Nome", "admin@redekrill.com", Role.ADMIN, true, null, "admin@redekrill.com")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void rejectsBlankEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Nome", "  ", Role.ADMIN, true, null, "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("E-mail do usuário é obrigatório.");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));
        when(userRepository.existsByEmailAndIdNot("outro@redekrill.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Nome", "outro@redekrill.com", Role.ADMIN, true, null, "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um usuário com o e-mail informado.");
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsShortNewPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user()));

        assertThatThrownBy(() -> useCase.execute(new UpdateUserCommand(
                1L, "Nome", "admin@redekrill.com", Role.ADMIN, true, "curta", "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("Senha deve ter pelo menos 8 caracteres.");
    }
}
