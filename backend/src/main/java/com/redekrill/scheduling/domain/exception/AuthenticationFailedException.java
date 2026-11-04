/*
 * Erro de credencial: e-mail e senha não batem, ou a sessão informada não vale mais.
 *
 * O handler global transforma isto em 401. A mensagem chega ao usuário, mas nunca
 * diz se o problema foi o usuário ou a senha — essa distinção abriria porta para
 * descobrir quem já tem cadastro no sistema.
 */
package com.redekrill.scheduling.domain.exception;

/**
 * Falha de autenticação. Vira 401 na resposta HTTP.
 */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
