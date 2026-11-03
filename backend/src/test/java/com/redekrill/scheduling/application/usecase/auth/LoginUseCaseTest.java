package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.SessionTokens;
import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.service.AuditService;
import com.redekrill.scheduling.application.service.AuthSessionService;
import com.redekrill.scheduling.application.service.LoginAttemptService;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.TooManyRequestsException;
import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private AuditService auditService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private AuthSessionService authSessionService;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepository, passwordHasher, auditService,
                loginAttemptService, authSessionService);
    }

    @Test
    void authenticatesIssuesSessionAndRecordsAudit() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(passwordHasher.matches("Admin@12345", "hashed-password")).thenReturn(true);
        when(authSessionService.start(TestFixtures.user(), null, null))
                .thenReturn(new SessionTokens("jwt-token", 900, "refresh-token", 604_800,
                        7L, UserResult.from(TestFixtures.user())));

        SessionTokens result = useCase.execute(
                new LoginCommand("  Admin@RedeKrill.com ", "Admin@12345"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.accessExpiresIn()).isEqualTo(900);
        assertThat(result.user().email()).isEqualTo("admin@redekrill.com");
        assertThat(result.user().role().name()).isEqualTo("ADMIN");
        verify(loginAttemptService).assertAllowed("admin@redekrill.com", null);
        verify(loginAttemptService).recordSuccess("admin@redekrill.com");
        verify(auditService).record(
                eq("admin@redekrill.com"), eq(AuditAction.LOGIN), eq("User"), isNull(),
                eq("Login realizado."));
    }

    @Test
    void rejectsMissingCommand() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("E-mail e senha são obrigatórios.");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> useCase.execute(new LoginCommand(" ", "senha")))
                .isInstanceOf(DomainException.class)
                .hasMessage("E-mail e senha são obrigatórios.");
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> useCase.execute(new LoginCommand("admin@redekrill.com", "")))
                .isInstanceOf(DomainException.class)
                .hasMessage("E-mail e senha são obrigatórios.");
    }

    @Test
    void rejectsUnknownUserAndRecordsFailure() {
        when(userRepository.findByEmail("ninguem@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("ninguem@redekrill.com", "senha")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(loginAttemptService).recordFailure("ninguem@redekrill.com", null);
        verify(auditService).record(
                eq("ninguem@redekrill.com"), eq(AuditAction.LOGIN_FAILED), eq("User"), isNull(),
                eq("Tentativa de login com usuário inexistente ou inativo."));
        verify(authSessionService, never()).start(any(), any(), any());
    }

    @Test
    void rejectsInactiveUser() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.inactiveUser()));

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("admin@redekrill.com", "senha")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(loginAttemptService).recordFailure("admin@redekrill.com", null);
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(passwordHasher.matches("errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("admin@redekrill.com", "errada")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos.");

        verify(passwordHasher).matches("errada", "hashed-password");
        verify(loginAttemptService).recordFailure("admin@redekrill.com", null);
        verify(authSessionService, never()).start(any(), any(), any());
    }

    @Test
    void rejectsTemporarilyBlockedLogin() {
        doThrow(new TooManyRequestsException("Muitas tentativas de login.", 60))
                .when(loginAttemptService).assertAllowed("admin@redekrill.com", "10.0.0.1");

        assertThatThrownBy(() -> useCase.execute(
                new LoginCommand("admin@redekrill.com", "Admin@12345", "10.0.0.1", "JUnit")))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Muitas tentativas de login.");

        verify(auditService).record(
                eq("admin@redekrill.com"), eq(AuditAction.LOGIN_BLOCKED), eq("User"), isNull(),
                any());
        verify(userRepository, never()).findByEmail(any());
    }
}
