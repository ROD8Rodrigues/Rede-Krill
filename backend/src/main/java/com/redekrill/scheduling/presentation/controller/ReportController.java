package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentReportUseCase;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AppointmentReportResult result = getAppointmentReportUseCase.execute(startDate, endDate);
        return WebMapper.toResponse(result);
    }
}
