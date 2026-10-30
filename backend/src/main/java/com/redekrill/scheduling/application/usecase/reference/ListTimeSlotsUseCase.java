package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

import java.util.List;

public class ListTimeSlotsUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public ListTimeSlotsUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<TimeSlotResult> execute() {
        return timeSlotRepository.findAll().stream()
                .map(TimeSlotResult::from)
                .toList();
    }
}
