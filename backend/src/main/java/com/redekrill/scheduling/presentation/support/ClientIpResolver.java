/*
 * De onde vem o IP que vai para a sessão e para o contador de bloqueio.
 *
 * O problema é que atrás de proxy o remoteAddr é o proxy, não o cliente, e a proteção de
 * login por IP não vale nada se todos os ataques parecerem vir da mesma máquina. A
 * solução não pode ser confiar cegamente em X-Forwarded-For: o cabeçalho é de quem chama,
 * e um atacante pulverizaria o contador de bloqueio inventando um endereço por tentativa
 * — ou colocaria uma string arbitrária numa coluna de quarenta e cinco caracteres.
 *
 * Por isso a dupla condição: o cabeçalho só é lido com app.security.trust-forwarded-
 * headers=true (ligado quando se sabe que há proxy na frente) e o primeiro item do
 * encadeamento ainda precisa ter forma de endereço. Qualquer recusa volta para
 * getRemoteAddr(), que é o que a conexão realmente disse.
 */
package com.redekrill.scheduling.presentation.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o endereço do cliente. X-Forwarded-For é header controlado por quem chama:
 * ele só vale quando a aplicação está atrás de proxy confiável
 * (app.security.trust-forwarded-headers=true) e quando o primeiro item do encadeamento
 * tem forma de endereço (dígitos, letras, ponto, dois-pontos e no máximo 45 caracteres).
 * Fora disso o endereço real da conexão é usado — evita estouro da coluna
 * auth_session.ip_address e contadores de bloqueio pulverizados por cabeçalho forjado.
 */
public final class ClientIpResolver {

    private static final int MAX_ADDRESS_LENGTH = 45;

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null) {
                String candidate = forwarded.split(",")[0].trim();
                if (looksLikeAddress(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean looksLikeAddress(String value) {
        if (value.isEmpty() || value.length() > MAX_ADDRESS_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean allowed = Character.isLetterOrDigit(character) || character == '.' || character == ':';
            if (!allowed) {
                return false;
            }
        }
        return value.indexOf('.') >= 0 || value.indexOf(':') >= 0;
    }
}
