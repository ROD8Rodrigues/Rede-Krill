/*
 * Pedido de cadastro de usuário feito por um ADMIN.
 *
 * Password é texto cru neste ponto do caminho, e é o único lugar onde ele existe: o
 * caso de uso valida a política, cifra antes de salvar e nunca devolve o hash em
 * resposta. A senha definida aqui faz o usuário nascer com a troca obrigatória ligada,
 * porque foi outra pessoa que a escolheu.
 *
 * role é o enum Role, não uma string: a apresentação já converte, e a aplicação não
 * aceita papel desconhecido.
 */
package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;

/**
 * Dados de entrada para criar um usuário.
 */
public record CreateUserCommand(
        String name,
        String email,
        String password,
        Role role,
        String actorEmail
) {
}
