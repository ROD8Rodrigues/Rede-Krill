package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.exception.ResourceNotFoundException;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAppointmentUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private GetAppointmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAppointmentUseCase(appointmentRepository);
    }

    @Test
    void returnsAppointment() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.appointment()));

        AppointmentResult result = useCase.execute(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.supplierName()).isEqualTo("Fornecedor Krill");
        assertThat(result.timeSlot().label()).isEqualTo("08:00 - 10:00");
        assertThat(result.palletizationType().capacityMultiplier()).isEqualTo(1);
    }

    @Test
    void rejectsMissingId() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Identificador do agendamento é obrigatório.");
    }

    @Test
    void rejectsUnknownAppointment() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Agendamento não encontrado.");
    }
}
