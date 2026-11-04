/*
 * Contrato de emissão de access token.
 *
 * O caso de uso sabe que existe um token com validade em segundos e não sabe que é
 * JWT. Não é ocultação por gosto: AuthSessionServiceTest injeta um TokenIssuer de
 * mentira e continua exercitando o fluxo inteiro — sessão gravada, token emitido,
 * auditoria registrada — sem chave de assinatura nem biblioteca de criptografia.
 *
 * O parâmetro sessionId entra porque o token precisa apontar para a linha de
 * AuthSession que ele revoga. O caminho, verificado no código: JwtTokenService grava
 * o valor na claim "sid", JwtAuthenticationFilter lê e confere se a sessão continua
 * ativa, e o logout usa esse mesmo id. Um token sem sid é descartado pelo filtro.
 */
package com.redekrill.scheduling.application.port;

import com.redekrill.scheduling.domain.model.User;

/**
 * Emissão de token de acesso. Implementada por JwtTokenService.
 */
public interface TokenIssuer {

    IssuedToken issue(User user, Long sessionId);
}
