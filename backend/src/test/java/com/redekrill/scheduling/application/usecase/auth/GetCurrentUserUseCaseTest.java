package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private GetCurrentUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentUserUseCase(userRepository);
    }

    @Test
    void returnsAuthenticatedUser() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.user()));

        UserResult result = useCase.execute("admin@redekrill.com");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Administrador");
        assertThat(result.email()).isEqualTo("admin@redekrill.com");
        assertThat(result.active()).isTrue();
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByEmail("ninguem@redekrill.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("ninguem@redekrill.com"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Usuário autenticado não encontrado ou inativo.");
    }

    @Test
    void rejectsInactiveUser() {
        when(userRepository.findByEmail("admin@redekrill.com"))
                .thenReturn(Optional.of(TestFixtures.inactiveUser()));

        assertThatThrownBy(() -> useCase.execute("admin@redekrill.com"))
                .isInstanceOf(AuthenticationFailedException.class);
    }
}
