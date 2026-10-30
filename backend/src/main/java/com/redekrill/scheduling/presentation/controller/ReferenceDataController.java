package com.redekrill.scheduling.presentation.controller;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.application.usecase.reference.ListPalletizationTypesUseCase;
import com.redekrill.scheduling.application.usecase.reference.ListTimeSlotsUseCase;
import com.redekrill.scheduling.presentation.mapper.WebMapper;
import com.redekrill.scheduling.presentation.response.PalletizationTypeResponse;
import com.redekrill.scheduling.presentation.response.TimeSlotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Dados de referência")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceDataController {

    private final ListPalletizationTypesUseCase listPalletizationTypesUseCase;
    private final ListTimeSlotsUseCase listTimeSlotsUseCase;

    public ReferenceDataController(ListPalletizationTypesUseCase listPalletizationTypesUseCase,
                                   ListTimeSlotsUseCase listTimeSlotsUseCase) {
        this.listPalletizationTypesUseCase = listPalletizationTypesUseCase;
        this.listTimeSlotsUseCase = listTimeSlotsUseCase;
    }

    @GetMapping("/palletization-types")
    @Operation(summary = "Lista os tipos de paletização")
    public List<PalletizationTypeResponse> palletizationTypes() {
        List<PalletizationTypeResult> result = listPalletizationTypesUseCase.execute();
        return result.stream().map(WebMapper::toResponse).toList();
    }

    @GetMapping("/time-slots")
    @Operation(summary = "Lista as quatro faixas horárias de 2 horas")
    public List<TimeSlotResponse> timeSlots() {
        List<TimeSlotResult> result = listTimeSlotsUseCase.execute();
        return result.stream().map(WebMapper::toResponse).toList();
    }
}
