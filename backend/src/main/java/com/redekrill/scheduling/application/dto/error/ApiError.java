/*
 * O corpo de erro que todo mundo devolve.
 *
 * Um formato só para dois mundos que geram erro: GlobalExceptionHandler, que traduz as
 * exceções de domínio e de validação, e os manipuladores de segurança, que respondem
 * antes de qualquer controller rodar. Os dois produzem esta mesma forma, então o
 * frontend tem um único caminho de leitura.
 *
 * fieldErrors é um mapa curto de campo -> mensagem, preenchido na validação e nulo no
 * resto. code é o que separa erros que chegam com o mesmo status: ACCESS_DENIED e
 * PASSWORD_CHANGE_REQUIRED são os dois 403, e sem código a distinção dependeria de ler
 * a frase da mensagem. RATE_LIMITED é o 429, sempre acompanhado de Retry-After no
 * cabeçalho. O construtor sem code existe porque a maioria dos erros de negócio não
 * precisa de código — a mensagem já basta.
 */
package com.redekrill.scheduling.application.dto.error;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de erro compartilhado entre a camada de aplicação/apresentação e os
 * manipuladores de segurança. Mantido sem dependências de framework.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        String code
) {

    public ApiError(Instant timestamp, int status, String error, String message,
                    String path, Map<String, String> fieldErrors) {
        this(timestamp, status, error, message, path, fieldErrors, null);
    }
}
