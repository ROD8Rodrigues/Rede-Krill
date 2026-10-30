package com.redekrill.scheduling.application.service;

import com.redekrill.scheduling.domain.model.AuditAction;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;

import java.time.Clock;
import java.time.Instant;

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
