package com.redekrill.scheduling.application.dto.auth;

/**
 * Nomes de atributos de requisição compartilhados entre filtros de
 * infraestrutura e controladores de apresentação. Mantidos fora da
 * infraestrutura para preservar a direção das dependências entre as camadas.
 */
public final class AuthRequestAttributes {

    public static final String MUST_CHANGE_PASSWORD = "mustChangePassword";
    public static final String SESSION_ID = "authSessionId";

    private AuthRequestAttributes() {
    }
}
