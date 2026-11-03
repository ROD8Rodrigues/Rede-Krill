package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(Long id);

    PageResult<Appointment> search(LocalDate startDate, LocalDate endDate, int page, int size);

    PageResult<Appointment> searchReport(LocalDate startDate, LocalDate endDate, int page, int size);

    List<Appointment> findByPeriod(LocalDate startDate, LocalDate endDate);

    long countByPeriod(LocalDate startDate, LocalDate endDate);

    long sumPalletsByPeriod(LocalDate startDate, LocalDate endDate);

    long sumEffectivePalletsByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Soma a quantidade declarada (sem multiplicador) dos agendamentos ativos do dia.
     */
    long sumDailyPalletsByDate(LocalDate date);

    /**
     * Soma a quantidade declarada dos agendamentos ativos do dia, excluindo um agendamento.
     */
    long sumDailyPalletsByDateExcluding(LocalDate date, Long appointmentId);

    /**
     * Soma a quantidade considerada na faixa (com multiplicador) dos agendamentos ativos.
     */
    long sumSlotPalletsByDateAndTimeSlot(LocalDate date, Long timeSlotId);

    /**
     * Soma a quantidade considerada na faixa, excluindo um agendamento.
     */
    long sumSlotPalletsByDateAndTimeSlotExcluding(LocalDate date, Long timeSlotId, Long appointmentId);

    boolean existsByOrderId(Long orderId);

    boolean existsByOrderIdAndIdNot(Long orderId, Long appointmentId);

    boolean existsByTruckTypeId(Long truckTypeId);

    boolean existsByTimeSlotId(Long timeSlotId);

    boolean existsByPalletizationTypeId(Long palletizationTypeId);
}
