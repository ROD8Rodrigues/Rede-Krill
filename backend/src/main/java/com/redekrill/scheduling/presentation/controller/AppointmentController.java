package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.appointment.AppointmentCommandResult;
import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.application.dto.appointment.CreateAppointmentCommand;
import com.redekrill.scheduling.application.dto.appointment.UpdateAppointmentCommand;
import com.redekrill.scheduling.application.usecase.appointment.CreateAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.DeleteAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.GetAppointmentUseCase;
import com.redekrill.scheduling.application.usecase.appointment.ListAppointmentsUseCase;
import com.redekrill.scheduling.application.usecase.appointment.UpdateAppointmentUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.CreateAppointmentRequest;
import com.redekrill.scheduling.presentation.request.UpdateAppointmentRequest;
import com.redekrill.scheduling.presentation.response.AppointmentCommandResponse;
import com.redekrill.scheduling.presentation.response.AppointmentResponse;
import com.redekrill.scheduling.presentation.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final CreateAppointmentUseCase createAppointmentUseCase;
    private final UpdateAppointmentUseCase updateAppointmentUseCase;
    private final DeleteAppointmentUseCase deleteAppointmentUseCase;
    private final GetAppointmentUseCase getAppointmentUseCase;
    private final ListAppointmentsUseCase listAppointmentsUseCase;

    public AppointmentController(CreateAppointmentUseCase createAppointmentUseCase,
                                 UpdateAppointmentUseCase updateAppointmentUseCase,
                                 DeleteAppointmentUseCase deleteAppointmentUseCase,
                                 GetAppointmentUseCase getAppointmentUseCase,
                                 ListAppointmentsUseCase listAppointmentsUseCase) {
        this.createAppointmentUseCase = createAppointmentUseCase;
        this.updateAppointmentUseCase = updateAppointmentUseCase;
        this.deleteAppointmentUseCase = deleteAppointmentUseCase;
        this.getAppointmentUseCase = getAppointmentUseCase;
        this.listAppointmentsUseCase = listAppointmentsUseCase;
    }

    @GetMapping
    @Operation(summary = "Lista agendamentos, com filtro opcional por período")
    public PageResponse<AppointmentResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return PageResponse.from(
                listAppointmentsUseCase.execute(startDate, endDate, page, size),
                WebMapper::toResponse
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um agendamento por identificador")
    public AppointmentResponse get(@PathVariable Long id) {
        AppointmentResult result = getAppointmentUseCase.execute(id);
        return WebMapper.toResponse(result);
    }

    @PostMapping
    @Operation(summary = "Cria um agendamento e envia e-mail de confirmação ao fornecedor")
    public ResponseEntity<AppointmentCommandResponse> create(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication authentication) {

        AppointmentCommandResult result = createAppointmentUseCase.execute(
                new CreateAppointmentCommand(
                        request.supplierName(),
                        request.supplierEmail(),
                        request.truckPlate(),
                        request.date(),
                        request.timeSlotId(),
                        request.truckTypeId(),
                        request.palletizationTypeId(),
                        request.palletQuantity(),
                        authentication.getName()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(WebMapper.toResponse(result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento")
    public AppointmentCommandResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest request,
            Authentication authentication) {

        AppointmentCommandResult result = updateAppointmentUseCase.execute(
                new UpdateAppointmentCommand(
                        id,
                        request.supplierName(),
                        request.supplierEmail(),
                        request.truckPlate(),
                        request.date(),
                        request.timeSlotId(),
                        request.truckTypeId(),
                        request.palletizationTypeId(),
                        request.palletQuantity(),
                        authentication.getName()
                )
        );

        return WebMapper.toResponse(result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um agendamento")
    public void delete(@PathVariable Long id, Authentication authentication) {
        deleteAppointmentUseCase.execute(id, authentication.getName());
    }
}
