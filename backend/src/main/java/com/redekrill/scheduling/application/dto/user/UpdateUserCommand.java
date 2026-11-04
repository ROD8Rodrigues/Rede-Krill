/*
 * Pedido de alteração de um usuário, com uma senha opcional dentro.
 *
 * Os campos de perfil — nome, e-mail, papel, ativo — seguem a criação. O que dá
 * caractere a este record é password: ele só aparece quando o ADMIN substitui a senha
 * de outra pessoa, e o caso de uso trata isso como reset, não como edição comum. Vindo
 * senha, a política é checada, o hash é trocado e a flag de troca obrigatória acende;
 * não vindo, a senha atual permanece no lugar.
 *
 * Um password presente também derruba as sessões do usuário. O pedido não diz isso,
 * mas é a consequência de quem troca a credencial por fora.
 */
package com.redekrill.scheduling.application.dto.user;

import com.redekrill.scheduling.domain.model.Role;

/**
 * Identificador, perfil e senha opcional de um usuário a atualizar.
 */
public record UpdateUserCommand(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        String password,
        String actorEmail
) {
}
