package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, Long> {

    List<TimeSlotJpaEntity> findAllByOrderByStartTimeAsc();
}
