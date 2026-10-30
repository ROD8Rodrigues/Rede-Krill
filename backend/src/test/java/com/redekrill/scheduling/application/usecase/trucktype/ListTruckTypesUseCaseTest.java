package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTruckTypesUseCaseTest {

    @Mock
    private TruckTypeRepository truckTypeRepository;

    private ListTruckTypesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListTruckTypesUseCase(truckTypeRepository);
    }

    @Test
    void listsTruckTypes() {
        when(truckTypeRepository.findAll()).thenReturn(List.of(TestFixtures.truckType()));

        List<TruckTypeResult> result = useCase.execute();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Carreta");
    }
}
