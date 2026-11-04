/*
 * Um agendamento pelo identificador interno, nada além disso.
 *
 * Os dois erros são diferentes de propósito: id nulo é forma (DomainException, que
 * vira 400), e id inexistente é fundo (ResourceNotFoundException, que vira 404). A
 * rota já exige papel adequado, então aqui não há checagem de dono: o agendamento pertence
 * à operação, não a quem o cadastrou.
 *
 * O status vem como está no registro, inclusive CANCELED. Filtrar cancelado é
 * decisão de quem consulta.
 */
package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;

/**
 * Leitura de um agendamento pelo id.
 */
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
