/*
 * A trilha de auditoria em páginas, do mais recente ao mais antigo.
 *
 * Sem filtro de nada — nem período, nem ator, nem tipo de evento. É a escolha
 * honesta para o escopo atual: a rota é exclusiva de ADMIN, a tabela cresce devagar,
 * e um buscador de log seria abstração sem consumo num sistema de um operador.
 * Se a trilha crescer, o filtro entra no repositório (audit_logs já tem as colunas),
 * não em memória aqui.
 *
 * A normalização de página vem de Pagination, então o teto de tamanho também se
 * aplica a esta leitura.
 */
package com.redekrill.scheduling.application.usecase.audit;

import com.redekrill.scheduling.application.dto.audit.AuditLogResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;

/**
 * Listagem paginada dos registros de auditoria.
 */
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
