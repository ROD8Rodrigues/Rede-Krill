/*
 * Entity de agendamento para o modelo de domínio, e só nesse sentido.
 *
 * Não existe toEntity aqui de propósito. A escrita de um agendamento é montada peça
 * a peça em JpaAppointmentRepositoryAdapter.save, porque ali as referências entram
 * como proxy de chave estrangeira (entityManager.getReference) em vez de entidades
 * carregadas. Fazer o caminho inverso por este mapper implicaria ter a entity em
 * mãos antes de salvar, que é justamente o que o adaptador evita.
 *
 * As três relações de cadastro e as duas de usuário viram modelos pelos mappers
 * correspondentes: o modelo Appointment espera TimeSlot, TruckType, PalletizationType
 * e User, não entidades JPA.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Conversão de leitura de AppointmentJpaEntity para o modelo Appointment.
 */
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
