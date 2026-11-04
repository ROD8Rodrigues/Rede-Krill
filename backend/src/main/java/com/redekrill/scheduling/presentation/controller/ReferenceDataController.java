/*
 * Paletização e faixas horárias no mesmo controller, porque as duas coisas têm a mesma
 * forma.
 *
 * São dados de referência: CRUD simples, sem estado que transite, sem política de
 * negócio além de unicidade de nome e de "não apaga se há agendamento vinculado". Juntar
 * os dois em um arquivo evita dois controllers de quarenta linhas que fariam a mesma
 * pergunta ao Spring.
 *
 * A leitura é liberada para qualquer autenticado — o formulário de agendamento precisa
 * das opções antes de criar qualquer coisa. A escrita é de ADMIN, e isso não está escrito
 * aqui: a regra mora na SecurityFilterChain, e a anotação @Operation só avisa quem lê o
 * Swagger.
 *
 * delete devolve 204 e o motivo da recusa, quando houver, vem do caso de uso como
 * ConflictException — que o GlobalExceptionHandler traduz em 409 com a frase certa.
 */
package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.reference.CreatePalletizationTypeCommand;
import com.redekrill.scheduling.application.dto.reference.CreateTimeSlotCommand;
import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.dto.reference.UpdatePalletizationTypeCommand;
import com.redekrill.scheduling.application.dto.reference.UpdateTimeSlotCommand;
import com.redekrill.scheduling.application.usecase.reference.CreatePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.CreateTimeSlotUseCase;
import com.redekrill.scheduling.application.usecase.reference.DeletePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.DeleteTimeSlotUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListPalletizationTypesUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListTimeSlotsUseCase;
import com.redekrill.scheduling.application.usecase.reference.UpdatePalletizationTypeUseCase;
import com.redekrill.scheduling.application.usecase.reference.UpdateTimeSlotUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.request.CreatePalletizationTypeRequest;
import com.redekrill.scheduling.presentation.request.CreateTimeSlotRequest;
import com.redekrill.scheduling.presentation.request.UpdatePalletizationTypeRequest;
import com.redekrill.scheduling.presentation.request.UpdateTimeSlotRequest;
import com.redekrill.scheduling.presentation.response.PalletizationTypeResponse;
import com.redekrill.scheduling.presentation.response.TimeSlotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas de dados de referência (tipos de paletização e faixas horárias). Não valida
 * conteúdo além do Bean Validation do request; coerência entre os dados é do caso de uso.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Dados de referência")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceDataController {

    private final ListPalletizationTypesUseCase listPalletizationTypesUseCase;
    private final ListTimeSlotsUseCase listTimeSlotsUseCase;
    private final CreatePalletizationTypeUseCase createPalletizationTypeUseCase;
    private final UpdatePalletizationTypeUseCase updatePalletizationTypeUseCase;
    private final DeletePalletizationTypeUseCase deletePalletizationTypeUseCase;
    private final CreateTimeSlotUseCase createTimeSlotUseCase;
    private final UpdateTimeSlotUseCase updateTimeSlotUseCase;
    private final DeleteTimeSlotUseCase deleteTimeSlotUseCase;

    public ReferenceDataController(ListPalletizationTypesUseCase listPalletizationTypesUseCase,
                                   ListTimeSlotsUseCase listTimeSlotsUseCase,
                                   CreatePalletizationTypeUseCase createPalletizationTypeUseCase,
                                   UpdatePalletizationTypeUseCase updatePalletizationTypeUseCase,
                                   DeletePalletizationTypeUseCase deletePalletizationTypeUseCase,
                                   CreateTimeSlotUseCase createTimeSlotUseCase,
                                   UpdateTimeSlotUseCase updateTimeSlotUseCase,
                                   DeleteTimeSlotUseCase deleteTimeSlotUseCase) {
        this.listPalletizationTypesUseCase = listPalletizationTypesUseCase;
        this.listTimeSlotsUseCase = listTimeSlotsUseCase;
        this.createPalletizationTypeUseCase = createPalletizationTypeUseCase;
        this.updatePalletizationTypeUseCase = updatePalletizationTypeUseCase;
        this.deletePalletizationTypeUseCase = deletePalletizationTypeUseCase;
        this.createTimeSlotUseCase = createTimeSlotUseCase;
        this.updateTimeSlotUseCase = updateTimeSlotUseCase;
        this.deleteTimeSlotUseCase = deleteTimeSlotUseCase;
    }

    @GetMapping("/palletization-types")
    @Operation(summary = "Lista os tipos de paletização")
    public List<PalletizationTypeResponse> palletizationTypes() {
        List<PalletizationTypeResult> result = listPalletizationTypesUseCase.execute();
        return result.stream().map(WebMapper::toResponse).toList();
    }

    @PostMapping("/palletization-types")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um tipo de paletização adicional (somente ADMIN)")
    public PalletizationTypeResponse createPalletizationType(
            @Valid @RequestBody CreatePalletizationTypeRequest request,
            Authentication authentication) {
        PalletizationTypeResult result = createPalletizationTypeUseCase.execute(
                new CreatePalletizationTypeCommand(
                        request.name(), request.capacityMultiplier(), authentication.getName()));
        return WebMapper.toResponse(result);
    }

    @PutMapping("/palletization-types/{id}")
    @Operation(summary = "Atualiza um tipo de paletização (somente ADMIN)")
    public PalletizationTypeResponse updatePalletizationType(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePalletizationTypeRequest request,
            Authentication authentication) {
        PalletizationTypeResult result = updatePalletizationTypeUseCase.execute(
                new UpdatePalletizationTypeCommand(
                        id, request.name(), request.capacityMultiplier(), authentication.getName()));
        return WebMapper.toResponse(result);
    }

    @DeleteMapping("/palletization-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um tipo de paletização sem agendamentos vinculados")
    public void deletePalletizationType(@PathVariable Long id, Authentication authentication) {
        deletePalletizationTypeUseCase.execute(id, authentication.getName());
    }

    @GetMapping("/time-slots")
    @Operation(summary = "Lista as faixas horárias de 2 horas")
    public List<TimeSlotResponse> timeSlots() {
        List<TimeSlotResult> result = listTimeSlotsUseCase.execute();
        return result.stream().map(WebMapper::toResponse).toList();
    }

    @PostMapping("/time-slots")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma faixa horária de 2 horas (somente ADMIN)")
    public TimeSlotResponse createTimeSlot(@Valid @RequestBody CreateTimeSlotRequest request,
                                           Authentication authentication) {
        TimeSlotResult result = createTimeSlotUseCase.execute(new CreateTimeSlotCommand(
                request.startTime(), request.endTime(), authentication.getName()));
        return WebMapper.toResponse(result);
    }

    @PutMapping("/time-slots/{id}")
    @Operation(summary = "Atualiza uma faixa horária (somente ADMIN)")
    public TimeSlotResponse updateTimeSlot(@PathVariable Long id,
                                           @Valid @RequestBody UpdateTimeSlotRequest request,
                                           Authentication authentication) {
        TimeSlotResult result = updateTimeSlotUseCase.execute(new UpdateTimeSlotCommand(
                id, request.startTime(), request.endTime(), authentication.getName()));
        return WebMapper.toResponse(result);
    }

    @DeleteMapping("/time-slots/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma faixa horária sem agendamentos vinculados")
    public void deleteTimeSlot(@PathVariable Long id, Authentication authentication) {
        deleteTimeSlotUseCase.execute(id, authentication.getName());
    }
}
