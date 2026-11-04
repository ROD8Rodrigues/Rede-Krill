/*
 * Faixa horária entre tabela e modelo, o menor dos mappers.
 *
 * Três campos e nenhuma conversão: LocalTime já é o tipo que o modelo usa. O
 * construtor do TimeSlot revalida ordem dos horários e duração de duas horas a cada
 * leitura, então uma linha fora da regra não passa despercebida — ela derruba a
 * consulta com a mensagem do domínio.
 *
 * É o mapper que a leitura de agendamento mais chama: AppointmentPersistenceMapper
 * resolve a faixa de cada linha lida por aqui.
 */
package com.redekrill.scheduling.infrastructure.persistence.mapper;

import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;

/**
 * Conversão entre TimeSlotJpaEntity e o modelo TimeSlot.
 */
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
