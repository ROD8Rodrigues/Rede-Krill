package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.CreateUserCommand;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private AuditService auditService;

    private CreateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateUserUseCase(
                userRepository, passwordHasher, auditService,
                new ImmediateTransactionRunner(),
                Clock.fixed(TestFixtures.NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsUserNormalizingEmailAndRecordingAudit() {
        when(passwordHasher.hash("Senha@123")).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, User.class).withId(5L));

        UserResult result = useCase.execute(new CreateUserCommand(
                "Operador", "  Operador@RedeKrill.com ", "Senha@123", Role.USER, "admin@redekrill.com"));

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.email()).isEqualTo("operador@redekrill.com");
        assertThat(result.role()).isEqualTo(Role.USER);
        assertThat(result.active()).isTrue();
        verify(auditService).record(
                eq("admin@redekrill.com"),
                eq(AuditAction.CREATE),
                eq("User"),
                eq(5L),
                contains("operador@redekrill.com")
        );
    }

    @Test
    void rejectsMissingCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Dados do usuário são obrigatórios.");
    }

    @Test
    void rejectsMissingRole() {
        assertThatThrownBy(() -> useCase.execute(new CreateUserCommand(
                "Operador", "operador@redekrill.com", "Senha@123", null, "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("Perfil do usuário é obrigatório.");
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> useCase.execute(new CreateUserCommand(
                "Operador", "operador@redekrill.com", "curta", Role.USER, "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("Senha deve ter pelo menos 8 caracteres.");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> useCase.execute(new CreateUserCommand(
                "Operador", "  ", "Senha@123", Role.USER, "admin@redekrill.com")))
                .isInstanceOf(DomainException.class)
                .hasMessage("E-mail do usuário é obrigatório.");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("operador@redekrill.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CreateUserCommand(
                "Operador", "Operador@RedeKrill.com", "Senha@123", Role.USER, "admin@redekrill.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um usuário com o e-mail informado.");
        verify(userRepository, never()).save(any());
    }
}
