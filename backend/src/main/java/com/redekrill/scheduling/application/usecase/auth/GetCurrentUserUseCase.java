/*
 * Quem é o usuário por trás deste token.
 *
 * Não é um lookup público: a entrada é o e-mail vindo do principal já autenticado.
 * Se essa pessoa não estiver mais na tabela, ou estiver inativa, a resposta é
 * AuthenticationFailedException, e não "não encontrado" — um 404 contaria que o
 * cadastro existe em outro lugar, e um usuário desativado com token ainda válido tem
 * de ouvir 401.
 *
 * É também o lugar onde o frontend busca a flag de troca obrigatória de senha ao
 * carregar a tela, sem depender de decodificar o token.
 */
package com.redekrill.scheduling.application.usecase.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.domain.exception.AuthenticationFailedException;
import com.redekrill.scheduling.domain.model.User;
import com.redekrill.scheduling.domain.repository.UserRepository;

/**
 * Perfil do usuário autenticado, a partir do e-mail do token.
 */
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
