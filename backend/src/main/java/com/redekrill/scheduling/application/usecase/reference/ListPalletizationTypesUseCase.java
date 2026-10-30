package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

import java.util.List;

public class ListPalletizationTypesUseCase {

    private final PalletizationTypeRepository palletizationTypeRepository;

    public ListPalletizationTypesUseCase(PalletizationTypeRepository palletizationTypeRepository) {
        this.palletizationTypeRepository = palletizationTypeRepository;
    }

    public List<PalletizationTypeResult> execute() {
        return palletizationTypeRepository.findAll().stream()
                .map(PalletizationTypeResult::from)
                .toList();
    }
}
