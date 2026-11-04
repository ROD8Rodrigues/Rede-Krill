/*
 * O que se recebe ao entrar, e o campo que pode ser nulo sem ser erro.
 *
 * token e tokenType só vêm preenchidos quando o modo "expor token no corpo" está ligado;
 * no fluxo por cookie, a autenticação está toda nas duas Set-Cookie da resposta e estes
 * campos chegam nulos de propósito. Um cliente que tratar token == null como falha de
 * login estaria confundindo segurança com falta de dado.
 *
 * expiresIn é a vida do access token em segundos — o mesmo valor que vai no claim de
 * expiração e no Max-Age da cookie, para que o cliente decida renovar antes do fim sem
 * depender de um 401 para descobrir.
 *
 * user é a conta autenticada, incluindo mustChangePassword, que é o que faz a aplicação
 * abrir direto na tela de troca de senha.
 */
package com.redekrill.scheduling.presentation.response;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
