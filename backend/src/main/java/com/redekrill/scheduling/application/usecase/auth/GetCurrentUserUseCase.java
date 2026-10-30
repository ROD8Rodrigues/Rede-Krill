package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResult execute(String email) {
        User user = userRepository.findByEmail(email)
                .filter(User::active)
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Usuário autenticado não encontrado ou inativo."));
        return UserResult.from(user);
    }
}
