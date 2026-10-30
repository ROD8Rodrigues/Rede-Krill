package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAppointmentsUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private ListAppointmentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListAppointmentsUseCase(appointmentRepository);
    }

    @Test
    void listsAppointmentsUsingProvidedPeriodAndPagination() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        when(appointmentRepository.search(start, end, 1, 20))
                .thenReturn(new PageResult<>(List.of(TestFixtures.appointment()), 1, 20, 1, 1));

        PageResult<AppointmentResult> result = useCase.execute(start, end, 1, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().supplierName()).isEqualTo("Fornecedor Krill");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void usesWidestPeriodAndClampsPaginationWhenNotProvided() {
        when(appointmentRepository.search(
                LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31), 0, 100))
                .thenReturn(new PageResult<>(List.of(), 0, 100, 0, 0));

        PageResult<AppointmentResult> result = useCase.execute(null, null, -2, 1_000);

        assertThat(result.content()).isEmpty();
        verify(appointmentRepository).search(
                LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31), 0, 100);
    }

    @Test
    void rejectsInvalidPeriod() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> useCase.execute(start, end, 0, 10))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial não pode ser posterior à data final.");
    }
}
