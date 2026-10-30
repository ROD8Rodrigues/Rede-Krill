package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.auth.LoginCommand;
import com.redekrill.scheduling.application.dto.auth.LoginResult;
import com.redekrill.scheduling.application.port.IssuedToken;
import com.redekrill.scheduling.application.port.PasswordHasher;
import com.redekrill.scheduling.application.port.TokenIssuer;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.exception.DomainException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private TokenIssuer tokenIssuer;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(userRepository, passwordHasher, tokenIssuer);
    }

    @Test
    void authenticatesAndIssuesToken() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));
        when(passwordHasher.matches("Admin@12345", "hashed-password")).thenReturn(true);
        when(tokenIssuer.issue(TestFixtures.user()))
                .thenReturn(new IssuedToken("jwt-token", 3_600));

        LoginResult result = useCase.execute(new LoginCommand("  Admin@RedeKrill.com ", "Admin@12345"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(3_600);
        assertThat(result.user().email()).isEqualTo("admin@redekrill.com");
        assertThat(result.user().role().name()).isEqualTo("ADMIN");
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
    void rejectsUnknownUser() {
        when(userRepository.findByEmail("ninguem@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("ninguem@redekrill.com", "senha")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    void rejectsInactiveUser() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.inactiveUser()));

        assertThatThrownBy(() -> useCase.execute(new LoginCommand("admin@redekrill.com", "senha")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("E-mail ou senha inválidos.");
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
    }
}
