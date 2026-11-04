/*
 * Uma linha da trilha de auditoria, do jeito que ela é guardada.
 *
 * actorEmail é o e-mail de quem agiu (ou o endereço anônimo usado pelos manipuladores de
 * segurança quando não havia identidade). entityId é String, não número: a coluna
 * registra id de entidades de tipos diferentes, e algumas ações não têm entidade
 * correspondente.
 *
 * details é texto livre montado por quem agiu — "Usuário atualizado: fulano@x.com", por
 * exemplo — o que registra o contexto da ação sem exigir coluna nova por tipo de evento.
 * É também a razão de a rota não oferecer filtro por conteúdo: o que está ali dentro foi
 * escrito pela operação, não por um vocabulário controlado.
 */
package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.AuditAction;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        AuditAction action,
        String entityName,
        String entityId,
        String details,
        Instant createdAt
) {
}
