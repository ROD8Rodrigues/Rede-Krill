package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    PageResult<AuditLog> findAll(int page, int size);
}
