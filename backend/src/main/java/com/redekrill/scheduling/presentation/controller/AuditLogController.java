/*
 * A única porta de saída da trilha de auditoria.
 *
 * Leitura pura, só ADMIN, paginada do mesmo jeito das outras listas. Não há filtro por
 * ação, ator ou período — a rota aceita page e size e devolve a trilha inteira, do mais
 * recente para o mais antigo. A superfície é pequena de propósito: cada filtro novo
 * pediria validação própria e um índice correspondente, e nada hoje justifica isso.
 *
 * A ordenação (mais recente primeiro) vem do caso de uso, não daqui. Um controller que
 * reordena a lista esconde do leitor onde a regra está, e esta rota é justamente o lugar
 * onde alguém vai procurar a verdade sobre quem fez o quê.
 */
package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.usecase.audit.ListAuditLogsUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.response.AuditLogResponse;
import com.redekrill.scheduling.presentation.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista logs de auditoria. Não escreve: registrar evento é responsabilidade dos casos de
 * uso que agem, nunca de quem consulta.
 */
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Auditoria")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final ListAuditLogsUseCase listAuditLogsUseCase;

    public AuditLogController(ListAuditLogsUseCase listAuditLogsUseCase) {
        this.listAuditLogsUseCase = listAuditLogsUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista os logs de auditoria (somente ADMIN)")
    public PageResponse<AuditLogResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return PageResponse.from(listAuditLogsUseCase.execute(page, size), WebMapper::toResponse);
    }
}
