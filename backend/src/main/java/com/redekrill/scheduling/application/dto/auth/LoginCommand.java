/*
 * A entrada do login: credencial mais o contexto de quem pediu.
 *
 * email e password são o que o usuário digita — LoginUseCase normaliza o e-mail para
 * minúsculas antes de procurar, e a comparação de senha é feita pelo hash, nunca
 * contra o texto.
 *
 * clientIp e userAgent existem para a rastreabilidade: viram colunas da sessão criada
 * e abastecem o contador de tentativas por endereço. O construtor de dois argumentos é
 * usado nos testes de regra de negócio, onde esses dois não mudam o resultado. IP
 * ausente não desarma a proteção: LoginAttemptService agrupa essas tentativas sob a
 * chave "desconhecido".
 */
package com.redekrill.scheduling.application.dto.auth;

/**
 * Credencial e origem da tentativa de login.
 */
public record LoginCommand(String email, String password, String clientIp, String userAgent) {

    public LoginCommand(String email, String password) {
        this(email, password, null, null);
    }
}
