package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

import java.util.List;

public class ListTruckTypesUseCase {

    private final TruckTypeRepository truckTypeRepository;

    public ListTruckTypesUseCase(TruckTypeRepository truckTypeRepository) {
        this.truckTypeRepository = truckTypeRepository;
    }

    public List<TruckTypeResult> execute() {
        return truckTypeRepository.findAll().stream()
                .map(TruckTypeResult::from)
                .toList();
    }
}
