/*
 * A fotografia de ocupação de um dia: quanto já foi somado no dia e em cada faixa.
 *
 * Este caso de uso não passa pela avaliação de capacidade de um lançamento. Ele lê
 * os dois limites fixos direto de CapacityPolicy e calcula o percentual com
 * occupancyPercentage — por isso a resposta não traz avisos, traz situação. Avisos
 * são produto do Create/Update, onde existe um agendamento novo para pesar na conta.
 *
 * São uma consulta de soma por faixa, além da soma do dia. Com a tabela de faixas
 * pequena e a tabela tem índice em (status, appointment_date), isso é aceitável; se o cadastro
 * de faixas crescer, o lugar de resolver é uma única consulta agrupada no
 * repositório, não uma pré-agregação em memória aqui.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.CapacityOverviewResult;
import com.redekrill.scheduling.application.dto.appointment.SlotCapacityResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.policy.CapacityPolicy;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Ocupação do dia e de cada faixa horária em uma data.
 */
public class GetCapacityOverviewUseCase {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;

    public GetCapacityOverviewUseCase(AppointmentRepository appointmentRepository,
                                      TimeSlotRepository timeSlotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public CapacityOverviewResult execute(LocalDate date) {
        if (date == null) {
            throw new DomainException("Data é obrigatória para consultar a capacidade.");
        }

        long dailyPallets = appointmentRepository.sumDailyPalletsByDate(date);
        List<TimeSlot> timeSlots = timeSlotRepository.findAll();
        List<SlotCapacityResult> slots = timeSlots.stream()
                .map(timeSlot -> toSlotResult(date, timeSlot))
                .toList();

        return new CapacityOverviewResult(
                date,
                dailyPallets,
                CapacityPolicy.DAILY_LIMIT,
                CapacityPolicy.occupancyPercentage(dailyPallets, CapacityPolicy.DAILY_LIMIT),
                slots
        );
    }

    private SlotCapacityResult toSlotResult(LocalDate date, TimeSlot timeSlot) {
        long pallets = appointmentRepository.sumSlotPalletsByDateAndTimeSlot(date, timeSlot.id());
        return new SlotCapacityResult(
                TimeSlotResult.from(timeSlot),
                pallets,
                CapacityPolicy.TIME_SLOT_LIMIT,
                CapacityPolicy.occupancyPercentage(pallets, CapacityPolicy.TIME_SLOT_LIMIT)
        );
    }
}
