/*
 * O usuário: a tabela onde mora a única credencial do sistema.
 *
 * password_hash é texto de 100 caracteres, e é sempre hash de BCrypt — nunca senha, nunca
 * cifra reversível. O tamanho tem folga porque o formato do BCrypt é fixo em 60, e a
 * coluna sobreviveria a uma troca de algoritmo sem reconstruir a tabela.
 *
 * must_change_password é a coluna que muda a experiência de quem entra: ela acende quando a
 * senha foi definida por outra pessoa (cadastro pelo ADMIN, reset por edição) e faz o
 * filtro de troca obrigatória segurar o usuário até ele escolher a própria senha.
 *
 * active desliga sem apagar. Contas ficam na tabela porque o histórico de agendamentos e de
 * auditoria referencia o e-mail da pessoa, e versão com @Version é o que impede duas
 * edições simultâneas de se sobrescreverem.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import com.redekrill.scheduling.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela users.
 */
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
