/*
 * O token emitido e o tempo que ele vale, sem nenhuma forma de representação.
 *
 * Dois campos e uma razão de existir: AuthSessionService precisa montar SessionTokens
 * com o prazo de expiração do access, mas não tem nada a fazer com claims, assinatura
 * nem algoritmo. Devolver o objeto montado pelo construtor de JWT arrastaria a
 * biblioteca para dentro da aplicação.
 */
package com.redekrill.scheduling.application.port;

/**
 * Par token/opacidade + segundos de validade.
 */
public record IssuedToken(String value, long expiresInSeconds) {
}
