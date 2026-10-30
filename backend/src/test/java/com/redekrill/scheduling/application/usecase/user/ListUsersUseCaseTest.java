package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private ListUsersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUsersUseCase(userRepository);
    }

    @Test
    void listsUsersWithNormalizedPagination() {
        when(userRepository.findAll(0, 100))
                .thenReturn(new PageResult<User>(List.of(TestFixtures.user()), 0, 100, 1, 1));

        PageResult<UserResult> result = useCase.execute(-1, 500);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().email()).isEqualTo("admin@redekrill.com");
        verify(userRepository).findAll(0, 100);
    }
}
