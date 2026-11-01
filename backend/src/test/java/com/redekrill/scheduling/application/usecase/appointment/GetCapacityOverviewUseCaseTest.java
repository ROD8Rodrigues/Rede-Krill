package com.redekrill.scheduling.application.usecase.appointment;

import com.redekrill.scheduling.application.dto.appointment.CapacityOverviewResult;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCapacityOverviewUseCaseTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;

    private GetCapacityOverviewUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCapacityOverviewUseCase(appointmentRepository, timeSlotRepository);
    }

    @Test
    void returnsDailyAndSlotOccupancyPercentages() {
        when(appointmentRepository.sumDailyPalletsByDate(TestFixtures.DATE)).thenReturn(240L);
        when(timeSlotRepository.findAll()).thenReturn(List.of(
                TestFixtures.timeSlot(), TestFixtures.otherTimeSlot()));
        when(appointmentRepository.sumSlotPalletsByDateAndTimeSlot(TestFixtures.DATE, 1L))
                .thenReturn(60L);
        when(appointmentRepository.sumSlotPalletsByDateAndTimeSlot(TestFixtures.DATE, 2L))
                .thenReturn(130L);

        CapacityOverviewResult result = useCase.execute(TestFixtures.DATE);

        assertThat(result.date()).isEqualTo(TestFixtures.DATE);
        assertThat(result.dailyPallets()).isEqualTo(240);
        assertThat(result.dailyLimit()).isEqualTo(480);
        assertThat(result.dailyOccupancyPercentage()).isEqualTo(50.0);
        assertThat(result.slots()).hasSize(2);
        assertThat(result.slots().get(0).timeSlot().id()).isEqualTo(1L);
        assertThat(result.slots().get(0).occupancyPercentage()).isEqualTo(50.0);
        assertThat(result.slots().get(1).pallets()).isEqualTo(130);
        assertThat(result.slots().get(1).occupancyPercentage()).isEqualTo(108.3);
    }

    @Test
    void rejectsMissingDate() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Data é obrigatória para consultar a capacidade.");
    }
}
