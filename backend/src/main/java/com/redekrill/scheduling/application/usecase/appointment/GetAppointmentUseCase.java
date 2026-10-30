package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

public class GetAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;

    public GetAppointmentUseCase(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public AppointmentResult execute(Long id) {
        if (id == null) {
            throw new DomainException("Identificador do agendamento é obrigatório.");
        }

        return appointmentRepository.findById(id)
                .map(AppointmentResult::from)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }
}
