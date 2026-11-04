package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.AppointmentReportResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
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
class GetAppointmentReportUseCaseTest {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

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
        when(appointmentRepository.searchReport(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE))
                .thenReturn(PageResult.of(List.of(
                        TestFixtures.appointment(1L, 10, TestFixtures.palletized()),
                        TestFixtures.appointment(2L, 10, TestFixtures.nonPalletized())
                ), DEFAULT_PAGE, DEFAULT_SIZE, 2, 1));
        when(appointmentRepository.countByPeriod(start, end)).thenReturn(2L);
        when(appointmentRepository.sumPalletsByPeriod(start, end)).thenReturn(20L);
        when(appointmentRepository.sumEffectivePalletsByPeriod(start, end)).thenReturn(30L);
        when(appointmentRepository.countCanceledByPeriod(start, end)).thenReturn(1L);

        AppointmentReportResult result = useCase.execute(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertThat(result.totalAppointments()).isEqualTo(2);
        assertThat(result.totalPallets()).isEqualTo(20);
        assertThat(result.totalEffectivePallets()).isEqualTo(30);
        assertThat(result.totalCanceled()).isEqualTo(1);
        assertThat(result.matchingAppointments()).isEqualTo(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(DEFAULT_SIZE);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.appointments()).hasSize(2);
        assertThat(result.appointments().get(1).status().name()).isEqualTo("ACTIVE");
        assertThat(result.startDate()).isEqualTo(start);
        assertThat(result.endDate()).isEqualTo(end);
    }

    @Test
    void keepsCanceledAppointmentsInTheReport() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.searchReport(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE))
                .thenReturn(PageResult.of(List.of(
                        TestFixtures.appointment(1L, 10, TestFixtures.palletized()),
                        TestFixtures.canceledAppointment()
                ), DEFAULT_PAGE, DEFAULT_SIZE, 2, 1));
        when(appointmentRepository.countByPeriod(start, end)).thenReturn(2L);

        AppointmentReportResult result = useCase.execute(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertThat(result.totalAppointments()).isEqualTo(2);
        assertThat(result.appointments())
                .anyMatch(item -> item.status().name().equals("CANCELED"));
    }

    @Test
    void returnsEmptyReportWhenNoAppointmentsExist() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.searchReport(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE))
                .thenReturn(PageResult.of(List.of(), DEFAULT_PAGE, DEFAULT_SIZE, 0, 0));
        when(appointmentRepository.countByPeriod(start, end)).thenReturn(0L);

        AppointmentReportResult result = useCase.execute(start, end, null, DEFAULT_PAGE, DEFAULT_SIZE);

        assertThat(result.totalAppointments()).isZero();
        assertThat(result.totalPallets()).isZero();
        assertThat(result.totalEffectivePallets()).isZero();
        assertThat(result.appointments()).isEmpty();
    }

    @Test
    void filtersTheTableWithoutChangingThePeriodKpis() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(appointmentRepository.searchReport(
                start, end, AppointmentStatus.CANCELED, DEFAULT_PAGE, DEFAULT_SIZE))
                .thenReturn(PageResult.of(
                        List.of(TestFixtures.canceledAppointment()), DEFAULT_PAGE, DEFAULT_SIZE, 1, 1));
        when(appointmentRepository.countByPeriod(start, end)).thenReturn(8L);
        when(appointmentRepository.countCanceledByPeriod(start, end)).thenReturn(3L);

        AppointmentReportResult result =
                useCase.execute(start, end, AppointmentStatus.CANCELED, DEFAULT_PAGE, DEFAULT_SIZE);

        verify(appointmentRepository).searchReport(
                start, end, AppointmentStatus.CANCELED, DEFAULT_PAGE, DEFAULT_SIZE);
        assertThat(result.matchingAppointments()).isEqualTo(1);
        assertThat(result.totalAppointments()).isEqualTo(8);
        assertThat(result.totalCanceled()).isEqualTo(3);
        assertThat(result.appointments()).hasSize(1);
    }

    @Test
    void rejectsMissingDates() {
        assertThatThrownBy(() -> useCase.execute(null, LocalDate.of(2026, 2, 28), null, DEFAULT_PAGE, DEFAULT_SIZE))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial e data final são obrigatórias.");

        assertThatThrownBy(() -> useCase.execute(LocalDate.of(2026, 2, 1), null, null, DEFAULT_PAGE, DEFAULT_SIZE))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial e data final são obrigatórias.");
    }

    @Test
    void rejectsInvertedPeriod() {
        assertThatThrownBy(() -> useCase.execute(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), null, DEFAULT_PAGE, DEFAULT_SIZE))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data inicial não pode ser posterior à data final.");
    }
}
