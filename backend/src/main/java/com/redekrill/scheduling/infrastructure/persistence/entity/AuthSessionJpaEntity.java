/*
 * A sessão de login, que é o que dá ao JWT um lugar para ser revogado.
 *
 * O campo central é refresh_token_hash, unique e com 64 caracteres: é o resumo SHA-256 em
 * hexadecimal do token de renovação. A linha nunca guarda o token cru — o que vira cookie
 * só existe no instante da emissão, e quem tiver acesso à tabela não consegue reinventar o
 * valor a partir do resumo.
 *
 * user_id é número simples, sem @ManyToOne: a revogação em cascata é um update coletivo por
 * usuário, e os dados da pessoa são lidos por UserRepository quando importa.
 *
 * Os três instantes separam os modos de morte de uma sessão. expires_at é a data natural,
 * revoked_at é morte antecipada (logout, rotação ou reuso detectado), e a linha sem
 * nenhum dos dois é a sessão viva que o filtro confere pelo claim sid.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "auth_session")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela auth_session.
 */
public class AuthSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
