package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.TimeSlot;

import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository {

    List<TimeSlot> findAll();

    Optional<TimeSlot> findById(Long id);

    TimeSlot save(TimeSlot timeSlot);

    void deleteById(Long id);
}
