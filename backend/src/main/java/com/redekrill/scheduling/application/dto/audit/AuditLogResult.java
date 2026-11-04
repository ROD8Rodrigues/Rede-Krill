/*
 * A linha de auditoria no formato em que ela é consultada.
 *
 * Espelha o modelo AuditLog campo a campo, com duas heranças do caminho de escrita
 * que vale a pena conhecer antes de mexer aqui. A primeira: entityId é texto, e não
 * número, porque o AuditService converte o Long para String antes de gravar. A
 * segunda: actorEmail nem sempre é um usuário do sistema — uma tentativa de login sem
 * e-mail informado é registrada sob o ator anônimo definido em LoginUseCase.
 *
 * Não há conversão de formato nem tradução de ação. Quem formata a tabela é a
 * apresentação.
 */
package com.redekrill.scheduling.application.dto.audit;

import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;

import java.time.Instant;

/**
 * Projeção de leitura de AuditLog, produzida pelo caso de uso de listagem.
 */
public record AuditLogResult(
        Long id,
        String actorEmail,
        AuditAction action,
        String entityName,
        String entityId,
        String details,
        Instant createdAt
) {

    public static AuditLogResult from(AuditLog auditLog) {
        return new AuditLogResult(
                auditLog.id(),
                auditLog.actorEmail(),
                auditLog.action(),
                auditLog.entityName(),
                auditLog.entityId(),
                auditLog.details(),
                auditLog.createdAt()
        );
    }
}
