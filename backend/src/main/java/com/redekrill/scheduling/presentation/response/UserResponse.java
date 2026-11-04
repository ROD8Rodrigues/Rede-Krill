/*
 * A conta sem segredo nenhum.
 *
 * Não há password, hash nem salt — o record nem tem onde colocar isso, e é assim que se
 * garante que uma rota nova de usuário não vaze credencial por esquecimento.
 *
 * mustChangePassword é o mesmo sinal que o filtro de senha inicial usa para responder 403
 * com PASSWORD_CHANGE_REQUIRED em tudo, menos em me, csrf, change-password e logout — o
 * campo está na resposta porque a informação já é aplicada na request, não porque o
 * cliente decida o que fazer com ela. createdAt é informativa; a conta não expira.
 *
 * role é o enum do domínio, serializado pelo nome, e active false tem efeito imediato: o
 * filtro de autenticação recusa a conta inativa mesmo com token ainda dentro da
 * expiração, então a flag não é só enfeite de listagem.
 */
package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean active,
        boolean mustChangePassword,
        Instant createdAt
) {
}
