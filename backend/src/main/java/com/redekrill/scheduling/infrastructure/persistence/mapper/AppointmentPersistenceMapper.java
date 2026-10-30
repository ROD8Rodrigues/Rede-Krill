package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;

public final class AppointmentPersistenceMapper {

    private AppointmentPersistenceMapper() {
    }

    public static Appointment toDomain(AppointmentJpaEntity entity) {
        return new Appointment(
                entity.getId(),
                entity.getSupplierName(),
                entity.getSupplierEmail(),
                entity.getTruckPlate(),
                entity.getAppointmentDate(),
                TimeSlotPersistenceMapper.toDomain(entity.getTimeSlot()),
                TruckTypePersistenceMapper.toDomain(entity.getTruckType()),
                PalletizationTypePersistenceMapper.toDomain(entity.getPalletizationType()),
                entity.getPalletQuantity(),
                entity.getEffectivePallets(),
                UserPersistenceMapper.toDomain(entity.getCreatedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
