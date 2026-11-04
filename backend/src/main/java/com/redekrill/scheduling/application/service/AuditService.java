/*
 * O ponto único onde uma ação vira linha de auditoria.
 *
 * Toda gravação passa por aqui por dois motivos. O primeiro é o relógio: o instante
 * vem de um Clock injetado, então um teste consegue fixar a data e conferir a linha
 * produzida. O segundo é a conversão do id — os casos de uso têm Long em mãos e a
 * coluna do log é texto, então a tradução acontece uma vez só, aqui.
 *
 * Não há método de consulta nem de edição. A leitura é responsabilidade de
 * ListAuditLogsUseCase, e o modelo AuditLog não oferece mutação: a trilha não se
 * reescreve.
 */
package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Gravação padronizada do log de auditoria, com relógio injetado.
 */
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    public void record(String actorEmail, AuditAction action, String entityName,
                       Long entityId, String details) {
        String id = entityId == null ? null : entityId.toString();
        AuditLog auditLog = AuditLog.create(actorEmail, action, entityName, id, details, Instant.now(clock));
        auditLogRepository.save(auditLog);
    }
}
