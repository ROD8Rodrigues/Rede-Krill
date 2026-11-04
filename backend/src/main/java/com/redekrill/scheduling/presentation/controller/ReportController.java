/*
 * Relatório de agendamentos por período — uma rota, e ela quase toda é parâmetro.
 *
 * Diferente da listagem comum, as duas datas são obrigatórias: um relatório sem período
 * signifícaria somar a tabela inteira, e a resposta mudaria de tamanho sozinha conforme o
 * banco cresce. Se faltar alguma, o MissingServletRequestParameterException vira 400 com
 * o nome do parâmetro antes de qualquer consulta.
 *
 * status é opcional e chega como o nome do enum. A resposta traz duas visões que
 * convivem: os consolidados do período (quantidade, pallets somados, pallets efetivos e
 * cancelados, todos ignorando o filtro de status) e a contagem das linhas que batem com
 * o filtro, junto da página pedida. Separar as duas é o que permite mostrar um total
 * estável acima de uma lista que muda conforme o seletor — sem que a tela precise somar
 * nada.
 *
 * Nenhuma conta é feita aqui: as somas vêm do repositório através do caso de uso, que
 * ainda recusa período invertido antes de consultar.
 */
package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentReportUseCase;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.response.AppointmentReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Expõe o relatório de agendamentos. Não agrega nem formata: quem soma é a aplicação, e
 * quem apresenta é WebMapper.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatórios")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final GetAppointmentReportUseCase getAppointmentReportUseCase;

    public ReportController(GetAppointmentReportUseCase getAppointmentReportUseCase) {
        this.getAppointmentReportUseCase = getAppointmentReportUseCase;
    }

    @GetMapping("/appointments")
    @Operation(summary = "Relatório de agendamentos por período")
    public AppointmentReportResponse appointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AppointmentStatus status) {

        AppointmentReportResult result =
                getAppointmentReportUseCase.execute(startDate, endDate, status, page, size);
        return WebMapper.toResponse(result);
    }
}
