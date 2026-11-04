/*
 * O rastro no banco de que alguém está logado.
 *
 * Sem esta linha, o JWT seria inrevogável: um token assinado vale até expirar, e
 * não adianta apagar do navegador de quem pediu logout. A cada login nasce uma
 * sessão; o access token leva o id dela na claim "sid", e o filtro confere esse
 * id aqui antes de aceitar a requisição. Revogada a linha, o token ainda
 * criptograficamente válido para de funcionar no pedido seguinte.
 *
 * O refresh token é a outra metade do arranjo. Ele nunca é guardado como veio:
 * apenas o resumo SHA-256 aparece neste registro. Assim, um vazamento da tabela
 * não entrega token utilizável — quem tem a lista de hashes não consegue
 * reproduzir o texto original que o navegador precisa apresentar.
 *
 * ipAddress e userAgent são opcionais porque o header pode não chegar (proxy que
 * remove, chamada interna). Os dois são texto de apoio à investigação, cortados
 * nos tamanhos da coluna; não participam de nenhuma decisão de acesso.
 */
package com.redekrill.scheduling.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Sessão renovável. O token de refresh nunca é persistido em claro: apenas o
 * hash SHA-256 é armazenado. O access token referencia o id desta sessão, o que
 * permite revogação imediata no logout.
 */
public record AuthSession(
        Long id,
        // Referencia User.id por número, não por objeto: a sessão não precisa da conta inteira.
        Long userId,
        // Resumo em hexadecimal do refresh token; o texto cru só existe no cookie do cliente.
        String refreshTokenHash,
        Instant createdAt,
        Instant expiresAt,
        // null enquanto a sessão está viva; preenchido por logout, rotação ou reuso detectado.
        Instant revokedAt,
        String ipAddress,
        String userAgent
) {

    public AuthSession {
        Objects.requireNonNull(userId, "Usuário da sessão é obrigatório.");
        refreshTokenHash = DomainValidation.requireText(
                refreshTokenHash, "Hash do token de refresh é obrigatório.");
        Objects.requireNonNull(createdAt, "Data de criação da sessão é obrigatória.");
        Objects.requireNonNull(expiresAt, "Data de expiração da sessão é obrigatória.");
        if (!expiresAt.isAfter(createdAt)) {
            throw new com.redekrill.scheduling.domain.exception.DomainException(
                    "Expiração da sessão deve ser posterior à criação.");
        }
        ipAddress = DomainValidation.optionalText(ipAddress, 64,
                "Endereço IP da sessão deve ter no máximo 64 caracteres.");
        userAgent = DomainValidation.optionalText(userAgent, 255,
                "User-Agent da sessão deve ter no máximo 255 caracteres.");
    }

    /**
     * Sessão recém-autenticada, ainda sem id e sem revogação.
     *
     * Recebe o hash, e não o token: quem monta este objeto é
     * application.service.AuthSessionService, que gera o token e calcula o resumo
     * na sequência. O texto cru só vive no cookie HttpOnly entregue ao navegador.
     *
     * @param userId            dono da sessão.
     * @param refreshTokenHash  resumo SHA-256 em hexadecimal.
     * @param now               instante de criação.
     * @param expiresAt         fim da janela de renovação; deve ser posterior a now.
     * @param ipAddress         origem do login, opcional.
     * @param userAgent         cliente que fez o login, opcional.
     * @return a sessão pronta para ser persistida.
     */
    public static AuthSession create(Long userId, String refreshTokenHash, Instant now,
                                     Instant expiresAt, String ipAddress, String userAgent) {
        return new AuthSession(null, userId, refreshTokenHash, now, expiresAt, null,
                ipAddress, userAgent);
    }

    /**
     * Diz se a sessão ainda autoriza uma requisição.
     *
     * As duas condições estão juntas de propósito: para quem pergunta, revogar e
     * vencer produzem a mesma recusa, mas as causas continuam em campos
     * separados — assim dá para saber depois se alguém fez logout ou se a sessão
     * apenas expirou.
     *
     * @param now instante comparado — injetado pelo chamador para o teste poder
     *            avançar o relógio.
     * @return true enquanto não houve revogação e a expiração ainda não chegou.
     */
    public boolean activeAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /**
     * Uma sessão revogada teve o token de refresh consumido por uma rotação ou por
     * logout. Reapresentar esse token é reuso, não expiração natural.
     */
    public boolean revoked() {
        return revokedAt != null;
    }

    public AuthSession withId(Long newId) {
        return new AuthSession(newId, userId, refreshTokenHash, createdAt, expiresAt, revokedAt,
                ipAddress, userAgent);
    }
}
