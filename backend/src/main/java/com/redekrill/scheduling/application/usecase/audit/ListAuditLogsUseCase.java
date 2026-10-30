package com.redekrill.scheduling.application.usecase.audit;

import com.redekrill.scheduling.application.dto.audit.AuditLogResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;

public class ListAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    public ListAuditLogsUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResult<AuditLogResult> execute(int page, int size) {
        PageResult<AuditLog> result = auditLogRepository.findAll(
                Pagination.page(page), Pagination.size(size));

        return PageResult.of(
                result.content().stream().map(AuditLogResult::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
