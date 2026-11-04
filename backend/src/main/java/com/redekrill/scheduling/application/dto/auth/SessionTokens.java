/*
 * O pacote que uma autenticação bem-sucedida devolve.
 *
 * Este é o único instante em que o refresh token existe em texto cru. AuthSessionService
 * gera o valor, guarda o resumo na linha de AuthSession e entrega o cru aqui; depois
 * que a apresentação transformar em cookie HttpOnly, ninguém mais vê o original.
 *
 * Os dois prazos são segundos e vêm de quem emite: o do access vem do token
 * efetivamente assinado, o do refresh vem da propriedade de configuração. sessionId
 * acompanha porque o logout precisa apontar para a linha que se quer revogar, e user
 * é o retrato de quem entrou — inclusive a flag de troca obrigatória de senha, que a
 * tela usa para empurrar o usuário para a troca antes de qualquer outra ação.
 */
package com.redekrill.scheduling.application.dto.auth;

import com.redekrill.scheduling.application.dto.user.UserResult;

/**
 * Par de tokens emitidos, prazos, sessão associada e usuário autenticado.
 */
public record SessionTokens(
        String accessToken,
        long accessExpiresIn,
        String refreshToken,
        long refreshExpiresIn,
        long sessionId,
        UserResult user
) {
}
