/*
 * Uma consulta, e ela não é a busca de sobreposição.
 *
 * findAllByOrderByStartTimeAsc é o método que o adaptador usa, e a ordenação por horário de
 * início é o que faz o seletor da tela de agendamento vir na sequência do dia.
 *
 * Poderia existir um "findByInterval" para checar conflito? Poderia, e não existe: a regra
 * de sobreposição tem um lugar só — TimeSlot.overlaps, no domínio — e verificar
 * intervalo em SQL criaria uma segunda definição que alguém, mais tarde, esqueceria de
 * manter igual à primeira. O adaptador lê todas as faixas e compara no modelo.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório Spring Data da entidade de faixa horária.
 */
public interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, Long> {

    List<TimeSlotJpaEntity> findAllByOrderByStartTimeAsc();
}
