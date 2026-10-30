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

    List<Appointment> findByPeriod(LocalDate startDate, LocalDate endDate);

    void deleteById(Long id);

    long sumEffectivePalletsByDate(LocalDate date);

    long sumEffectivePalletsByDateExcluding(LocalDate date, Long appointmentId);

    long sumEffectivePalletsByDateAndTimeSlot(LocalDate date, Long timeSlotId);

    long sumEffectivePalletsByDateAndTimeSlotExcluding(LocalDate date, Long timeSlotId, Long appointmentId);

    boolean existsByTruckTypeId(Long truckTypeId);
}
