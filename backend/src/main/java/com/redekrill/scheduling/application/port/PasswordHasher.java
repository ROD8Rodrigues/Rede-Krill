/*
 * Contrato de cifra e comparação de senha.
 *
 * hash devolve o texto cifrado; matches responde se uma senha bate com um hash. Os
 * casos de uso usam os dois sem saber o algoritmo, e é isso que permite ajustar a
 * força do custo — app.security.bcrypt-strength, faixa aceita de 10 a 20 — sem
 * tocar em LoginUseCase, CreateUserUseCase ou ChangePasswordUseCase.
 *
 * matches recebe a senha em claro e o hash, e não o contrário, porque a comparação
 * de senha com hash é sempre assim: recalcula-se o hash do candidato com os mesmos
 * parâmetros embutidos no texto armazenado e compara-se. Não existe "descriptografar
 * o hash".
 */
package com.redekrill.scheduling.application.port;

/**
 * Cifra de senha sob controle da aplicação. Implementada por
 * BCryptPasswordHasherAdapter.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
