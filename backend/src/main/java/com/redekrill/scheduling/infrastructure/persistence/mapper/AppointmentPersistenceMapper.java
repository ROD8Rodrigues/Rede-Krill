package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;

public final class AppointmentPersistenceMapper {

    private AppointmentPersistenceMapper() {
    }

    public static Appointment toDomain(AppointmentJpaEntity entity) {
        UserJpaEntity canceledBy = entity.getCanceledBy();

        return new Appointment(
                entity.getId(),
                entity.getOrderId(),
                entity.getSupplierName(),
                entity.getSupplierEmail(),
                entity.getTruckPlate(),
                entity.getAppointmentDate(),
                TimeSlotPersistenceMapper.toDomain(entity.getTimeSlot()),
                TruckTypePersistenceMapper.toDomain(entity.getTruckType()),
                PalletizationTypePersistenceMapper.toDomain(entity.getPalletizationType()),
                entity.getPalletQuantity(),
                entity.getEffectivePallets(),
                entity.getNotes(),
                entity.getStatus(),
                canceledBy == null ? null : UserPersistenceMapper.toDomain(canceledBy),
                entity.getCanceledAt(),
                UserPersistenceMapper.toDomain(entity.getCreatedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
