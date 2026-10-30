package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    @Test
    void calculatesEffectivePalletsForPalletizedCargo() {
        Appointment appointment = Appointment.create(
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, TestFixtures.user(), TestFixtures.NOW);

        assertThat(appointment.effectivePallets()).isEqualTo(10);
        assertThat(appointment.truckPlate()).isEqualTo("ABC1D23");
        assertThat(appointment.supplierEmail()).isEqualTo("fornecedor@example.com");
    }

    @Test
    void doublesEffectivePalletsForNonPalletizedCargo() {
        Appointment appointment = Appointment.create(
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.nonPalletized(),
                10, TestFixtures.user(), TestFixtures.NOW);

        assertThat(appointment.effectivePallets()).isEqualTo(20);
    }

    @Test
    void recalculatesEffectivePalletsOnUpdate() {
        Appointment updated = TestFixtures.appointment().update(
                "Outro Fornecedor", "outro@example.com", "xyz9z99", TestFixtures.DATE,
                TestFixtures.otherTimeSlot(), TestFixtures.truckType(), TestFixtures.nonPalletized(),
                7, TestFixtures.NOW.plusSeconds(60));

        assertThat(updated.effectivePallets()).isEqualTo(14);
        assertThat(updated.palletQuantity()).isEqualTo(7);
        assertThat(updated.createdAt()).isEqualTo(TestFixtures.NOW);
        assertThat(updated.updatedAt()).isEqualTo(TestFixtures.NOW.plusSeconds(60));
    }

    @Test
    void rejectsInvalidQuantity() {
        assertThatThrownBy(() -> Appointment.create(
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                0, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("Quantidade de pallets deve ser maior que zero.");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> Appointment.create(
                "Fornecedor", "email-invalido", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("E-mail inválido");
    }
}
