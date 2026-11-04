/*
 * A faixa horária: três colunas, e a mais importante é a que não existe.
 *
 * Não há dia, unidade nem duração na tabela. Uma faixa é um intervalo do dia, válido para
 * todas as datas. O banco já segura o óbvio — horário único em (start_time, end_time) e
 * início antes do fim — e a duração de duas horas é regra só do modelo TimeSlot, revalidada
 * a cada leitura.
 *
 * LocalTime cai direto em time without time zone. A conversão de fuso é decisão de quem
 * escreve a data do agendamento, não desta tabela.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "time_slot")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela time_slot.
 */
public class TimeSlotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
