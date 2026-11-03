package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;

public final class TimeSlotPersistenceMapper {

    private TimeSlotPersistenceMapper() {
    }

    public static TimeSlot toDomain(TimeSlotJpaEntity entity) {
        return new TimeSlot(entity.getId(), entity.getStartTime(), entity.getEndTime());
    }

    public static TimeSlotJpaEntity toEntity(TimeSlot timeSlot) {
        TimeSlotJpaEntity entity = new TimeSlotJpaEntity();
        entity.setId(timeSlot.id());
        entity.setStartTime(timeSlot.startTime());
        entity.setEndTime(timeSlot.endTime());
        return entity;
    }
}
