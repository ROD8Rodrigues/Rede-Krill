package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.TimeSlotPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.TimeSlotJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaTimeSlotRepositoryAdapter implements TimeSlotRepository {

    private final TimeSlotJpaRepository repository;

    public JpaTimeSlotRepositoryAdapter(TimeSlotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TimeSlot> findAll() {
        return repository.findAllByOrderByStartTimeAsc().stream()
                .map(TimeSlotPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TimeSlot> findById(Long id) {
        return repository.findById(id).map(TimeSlotPersistenceMapper::toDomain);
    }
}
