/*
 * As consultas de agendamento, incluindo as que a camada de domínio não pode saber
 * que existem.
 *
 * Três grupos, cada um com uma razão própria de existir:
 * - as leituras com @EntityGraph (findDetailedById, search, searchReport,
 *   searchReportByStatus), porque montar o modelo de domínio toca cinco relações e, sem o
 *   graph, cada uma viraria um SELECT por linha;
 * - as somas de capacidade, que filtram status = ACTIVE dentro da query e por isso o
 *   cancelamento libera espaço sem apagar nada; as variantes Excluding servem à edição,
 *   onde o registro em edição já está na tabela e não pode contar duas vezes;
 * - os exists, que respondem por chave estrangeira antes de um delete ou de um cadastro.
 *
 * Duas notas de Postgres que já custaram tempo:
 * - search abre pela data mais alta e searchReport pela mais baixa, porque um alimenta a
 *   tela e o outro é leitura cronológica de relatório;
 * - searchReport e searchReportByStatus são consultas separadas de propósito. Passar enum
 *   nulo em "(:status is null or ...)" faz o driver inferir tipo e quebrar em runtime.
 *
 * As somas de período devolvem primitive porque usam coalesce dentro do JPQL; as de
 * capacidade devolvem Long embrulhado, e quem converte null em zero é o adaptador.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repositório Spring Data da entidade de agendamento.
 */
public interface AppointmentJpaRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    @EntityGraph(attributePaths = {"timeSlot", "truckType", "palletizationType", "createdBy", "canceledBy"})
    @Query("select a from AppointmentJpaEntity a where a.id = :id")
    Optional<AppointmentJpaEntity> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"timeSlot", "truckType", "palletizationType", "createdBy", "canceledBy"})
    @Query("""
            select a from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            order by a.appointmentDate desc, a.timeSlot.startTime desc, a.id desc
            """)
    Page<AppointmentJpaEntity> search(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      Pageable pageable);

    @Query("""
            select sum(a.palletQuantity) from AppointmentJpaEntity a
            where a.appointmentDate = :date
              and a.status = com.redekrill.scheduling.domain.model.AppointmentStatus.ACTIVE
            """)
    Long sumDailyPalletsByDate(@Param("date") LocalDate date);

    @Query("""
            select sum(a.palletQuantity) from AppointmentJpaEntity a
            where a.appointmentDate = :date
              and a.status = com.redekrill.scheduling.domain.model.AppointmentStatus.ACTIVE
              and a.id <> :appointmentId
            """)
    Long sumDailyPalletsByDateExcluding(@Param("date") LocalDate date,
                                        @Param("appointmentId") Long appointmentId);

    @Query("""
            select sum(a.effectivePallets) from AppointmentJpaEntity a
            where a.appointmentDate = :date
              and a.timeSlot.id = :timeSlotId
              and a.status = com.redekrill.scheduling.domain.model.AppointmentStatus.ACTIVE
            """)
    Long sumSlotPalletsByDateAndTimeSlot(@Param("date") LocalDate date,
                                         @Param("timeSlotId") Long timeSlotId);

    @Query("""
            select sum(a.effectivePallets) from AppointmentJpaEntity a
            where a.appointmentDate = :date
              and a.timeSlot.id = :timeSlotId
              and a.status = com.redekrill.scheduling.domain.model.AppointmentStatus.ACTIVE
              and a.id <> :appointmentId
            """)
    Long sumSlotPalletsByDateAndTimeSlotExcluding(@Param("date") LocalDate date,
                                                  @Param("timeSlotId") Long timeSlotId,
                                                  @Param("appointmentId") Long appointmentId);

    @EntityGraph(attributePaths = {"timeSlot", "truckType", "palletizationType", "createdBy", "canceledBy"})
    @Query("""
            select a from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            order by a.appointmentDate asc, a.timeSlot.startTime asc, a.id asc
            """)
    Page<AppointmentJpaEntity> searchReport(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            Pageable pageable);

    // Duas consultas explícitas: passar um enum nulo em "(:status is null or ...)"
    // obriga o Postgres a inferir o tipo do parâmetro e quebra em runtime.
    @EntityGraph(attributePaths = {"timeSlot", "truckType", "palletizationType", "createdBy", "canceledBy"})
    @Query("""
            select a from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
              and a.status = :status
            order by a.appointmentDate asc, a.timeSlot.startTime asc, a.id asc
            """)
    Page<AppointmentJpaEntity> searchReportByStatus(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate,
                                                    @Param("status") AppointmentStatus status,
                                                    Pageable pageable);

    @Query("""
            select count(a) from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            """)
    long countByPeriod(@Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(a.palletQuantity), 0) from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            """)
    long sumPalletsByPeriod(@Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("""
            select coalesce(sum(a.effectivePallets), 0) from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            """)
    long sumEffectivePalletsByPeriod(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
            select count(a) from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
              and a.status = com.redekrill.scheduling.domain.model.AppointmentStatus.CANCELED
            """)
    long countCanceledByPeriod(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndIdNot(Long orderId, Long appointmentId);

    boolean existsByTruckTypeId(Long truckTypeId);

    boolean existsByTimeSlotId(Long timeSlotId);

    boolean existsByPalletizationTypeId(Long palletizationTypeId);
}
