/*
 * A lista de usuários para o console de ADMIN.
 *
 * Paginada, ao contrário dos cadastros de referência — contas de acesso também crescem sem
 * limite natural, então a página entra aqui do mesmo jeito que entra na listagem de
 * agendamentos. Nenhum filtro: a tela mostra quem está ativo e quem não está, e a flag de troca
 * obrigatória é o que faz o roteador empurrar a pessoa para a troca de senha.
 *
 * O que não sai daqui é o que nunca sai de lugar nenhum: o hash. UserResult não tem o
 * campo, então não existe caminho nesta camada que exponha a credencial cifrada.
 */
package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.UserRepository;

/**
 * Listagem paginada de usuários, sem credenciais.
 */
public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResult<UserResult> execute(int page, int size) {
        PageResult<com.redekrill.scheduling.domain.model.User> result =
                userRepository.findAll(Pagination.page(page), Pagination.size(size));

        return PageResult.of(
                result.content().stream().map(UserResult::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
