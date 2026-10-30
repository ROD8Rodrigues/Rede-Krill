package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataUseCasesTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private PalletizationTypeRepository palletizationTypeRepository;

    @Test
    void listsTimeSlots() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(TestFixtures.timeSlot()));

        List<TimeSlotResult> result = new ListTimeSlotsUseCase(timeSlotRepository).execute();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().label()).isEqualTo("08:00 - 10:00");
    }

    @Test
    void listsPalletizationTypes() {
        when(palletizationTypeRepository.findAll())
                .thenReturn(List.of(TestFixtures.palletized(), TestFixtures.nonPalletized()));

        List<PalletizationTypeResult> result =
                new ListPalletizationTypesUseCase(palletizationTypeRepository).execute();

        assertThat(result).hasSize(2);
        assertThat(result.get(1).capacityMultiplier()).isEqualTo(2);
    }
}
