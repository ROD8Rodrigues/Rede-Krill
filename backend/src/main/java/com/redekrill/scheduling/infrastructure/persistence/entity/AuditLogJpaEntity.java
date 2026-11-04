/*
 * A linha de auditoria, escrita uma vez e lida para sempre.
 *
 * Sete colunas e nenhuma relação: actorEmail, entityName e entityId são texto, não chave
 * estrangeira. É a escolha que faz a trilha sobreviver ao que ela descreve — um tipo de
 * caminhão removido continua com o histórico de quem o criou, porque nada aponta para a
 * linha apagada.
 *
 * action é enumerado por nome. details é TEXT porque guarda a frase montada pelo caso de
 * uso, e frase de auditoria não cabe em 80 caracteres.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import com.redekrill.scheduling.domain.model.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela audit_log.
 */
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email", nullable = false, length = 160)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "entity_name", nullable = false, length = 60)
    private String entityName;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
