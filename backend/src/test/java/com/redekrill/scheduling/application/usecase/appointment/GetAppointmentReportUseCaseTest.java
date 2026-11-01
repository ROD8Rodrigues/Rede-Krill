package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.domain.exception.DomainException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAppointmentReportUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private GetAppointmentReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAppointmentReportUseCase(appointmentRepository);
    }

    @Test
    void aggregatesAppointmentsAndBothPalletTotals() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.findByPeriod(start, end)).thenReturn(List.of(
                TestFixtures.appointment(1L, 10, TestFixtures.palletized()),
                TestFixtures.appointment(2L, 10, TestFixtures.nonPalletized())
        ));

        AppointmentReportResult result = useCase.execute(start, end);

        assertThat(result.totalAppointments()).isEqualTo(2);
        assertThat(result.totalPallets()).isEqualTo(20);
        assertThat(result.totalEffectivePallets()).isEqualTo(30);
        assertThat(result.appointments()).hasSize(2);
        assertThat(result.appointments().get(1).status().name()).isEqualTo("ACTIVE");
        assertThat(result.startDate()).isEqualTo(start);
        assertThat(result.endDate()).isEqualTo(end);
    }

    @Test
    void keepsCanceledAppointmentsInTheReport() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.findByPeriod(start, end)).thenReturn(List.of(
                TestFixtures.appointment(1L, 10, TestFixtures.palletized()),
                TestFixtures.canceledAppointment()
        ));

        AppointmentReportResult result = useCase.execute(start, end);

        assertThat(result.totalAppointments()).isEqualTo(2);
        assertThat(result.appointments())
                .anyMatch(item -> item.status().name().equals("CANCELED"));
    }

    @Test
    void returnsEmptyReportWhenNoAppointmentsExist() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.findByPeriod(start, end)).thenReturn(List.of());

        AppointmentReportResult result = useCase.execute(start, end);

        assertThat(result.totalAppointments()).isZero();
        assertThat(result.totalPallets()).isZero();
        assertThat(result.totalEffectivePallets()).isZero();
        assertThat(result.appointments()).isEmpty();
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> useCase.execute(null, LocalDate.of(2026, 2, 28)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial e data final são obrigatórias.");

        assertThatThrownBy(() -> useCase.execute(LocalDate.of(2026, 2, 1), null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial e data final são obrigatórias.");
    }

    @Test
    void rejectsInvertedPeriod() {
        assertThatThrownBy(() -> useCase.execute(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial não pode ser posterior à data final.");
    }
}
