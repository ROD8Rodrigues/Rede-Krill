package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {"timeSlot", "truckType", "palletizationType", "createdBy", "canceledBy"})
    @Query("""
            select a from AppointmentJpaEntity a
            where a.appointmentDate between :startDate and :endDate
            order by a.appointmentDate asc, a.timeSlot.startTime asc, a.id asc
            """)
    List<AppointmentJpaEntity> findByPeriod(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

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

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndIdNot(Long orderId, Long appointmentId);

    boolean existsByTruckTypeId(Long truckTypeId);

    boolean existsByTimeSlotId(Long timeSlotId);

    boolean existsByPalletizationTypeId(Long palletizationTypeId);
}
