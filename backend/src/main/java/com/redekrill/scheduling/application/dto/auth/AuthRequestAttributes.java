/*
 * Nomes estáveis para dois dados que atravessam o filtro e chegam ao controller.
 *
 * O fluxo é sempre o mesmo: JwtAuthenticationFilter valida o token, lê o payload e
 * grava os dois atributos na requisição. PasswordChangeRequiredFilter consulta MUST_CHANGE_PASSWORD
 * para derrubar quem ainda usa a senha inicial, e o logout em AuthController lê
 * SESSION_ID para revogar exatamente a sessão daquele token.
 *
 * São nomes de atributo interno, não cabeçalhos HTTP — quem manda "mustChangePassword"
 * no request não ganha nada. As duas strings ficam aqui porque, se filtro e controller
 * escreverem nomes diferentes, o efeito é silencioso: a troca obrigatória deixa de
 * bloquear e o logout para de revogar.
 *
 * Classe final, construtor privado: só guarda constantes.
 */
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
