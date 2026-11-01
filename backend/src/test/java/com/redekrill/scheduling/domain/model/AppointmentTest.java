package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.ConflictException;
import com.redekrill.scheduling.domain.exception.DomainException;
import com.redekrill.scheduling.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    @Test
    void calculatesEffectivePalletsForPalletizedCargo() {
        Appointment appointment = Appointment.create(
                TestFixtures.ORDER_ID,
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, "Observação", TestFixtures.user(), TestFixtures.NOW);

        assertThat(appointment.orderId()).isEqualTo(TestFixtures.ORDER_ID);
        assertThat(appointment.effectivePallets()).isEqualTo(10);
        assertThat(appointment.dailyPallets()).isEqualTo(10);
        assertThat(appointment.slotPallets()).isEqualTo(10);
        assertThat(appointment.notes()).isEqualTo("Observação");
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.ACTIVE);
        assertThat(appointment.truckPlate()).isEqualTo("ABC1D23");
        assertThat(appointment.supplierEmail()).isEqualTo("fornecedor@example.com");
    }

    @Test
    void doublesOnlyTheSlotPalletsForNonPalletizedCargo() {
        Appointment appointment = Appointment.create(
                TestFixtures.ORDER_ID,
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.nonPalletized(),
                10, null, TestFixtures.user(), TestFixtures.NOW);

        assertThat(appointment.palletQuantity()).isEqualTo(10);
        assertThat(appointment.dailyPallets()).isEqualTo(10);
        assertThat(appointment.effectivePallets()).isEqualTo(20);
        assertThat(appointment.slotPallets()).isEqualTo(20);
        assertThat(appointment.notes()).isNull();
    }

    @Test
    void recalculatesPalletsOnUpdate() {
        Appointment updated = TestFixtures.appointment().update(
                2000L, "Outro Fornecedor", "outro@example.com", "xyz9z99", TestFixtures.DATE,
                TestFixtures.otherTimeSlot(), TestFixtures.truckType(), TestFixtures.nonPalletized(),
                7, "Atualizado", TestFixtures.NOW.plusSeconds(60));

        assertThat(updated.orderId()).isEqualTo(2000L);
        assertThat(updated.effectivePallets()).isEqualTo(14);
        assertThat(updated.slotPallets()).isEqualTo(14);
        assertThat(updated.dailyPallets()).isEqualTo(7);
        assertThat(updated.palletQuantity()).isEqualTo(7);
        assertThat(updated.notes()).isEqualTo("Atualizado");
        assertThat(updated.createdAt()).isEqualTo(TestFixtures.NOW);
        assertThat(updated.updatedAt()).isEqualTo(TestFixtures.NOW.plusSeconds(60));
    }

    @Test
    void cancelsPreservingTheOriginalData() {
        Appointment appointment = TestFixtures.appointment();

        Appointment canceled = appointment.cancel(TestFixtures.user(), TestFixtures.NOW.plusSeconds(30));

        assertThat(canceled.status()).isEqualTo(AppointmentStatus.CANCELED);
        assertThat(canceled.canceledBy()).isEqualTo(TestFixtures.user());
        assertThat(canceled.canceledAt()).isEqualTo(TestFixtures.NOW.plusSeconds(30));
        assertThat(canceled.orderId()).isEqualTo(appointment.orderId());
        assertThat(canceled.supplierName()).isEqualTo(appointment.supplierName());
        assertThat(canceled.dailyPallets()).isEqualTo(appointment.dailyPallets());
        assertThat(canceled.slotPallets()).isEqualTo(appointment.slotPallets());
    }

    @Test
    void rejectsRepeatedCancellation() {
        Appointment canceled = TestFixtures.canceledAppointment();

        assertThatThrownBy(() -> canceled.cancel(TestFixtures.user(), TestFixtures.NOW.plusSeconds(60)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Agendamento já está cancelado.");
    }

    @Test
    void rejectsUpdateOfCanceledAppointment() {
        Appointment canceled = TestFixtures.canceledAppointment();

        assertThatThrownBy(() -> canceled.update(
                2000L, "Fornecedor", "fornecedor@example.com", "ABC1D23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, null, TestFixtures.NOW.plusSeconds(60)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Agendamento cancelado não pode ser alterado.");
    }

    @Test
    void rejectsInvalidQuantity() {
        assertThatThrownBy(() -> Appointment.create(
                TestFixtures.ORDER_ID,
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                0, null, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("Quantidade de pallets deve ser maior que zero.");
    }

    @Test
    void rejectsMissingOrderId() {
        assertThatThrownBy(() -> Appointment.create(
                null,
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, null, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("ID Pedido é obrigatório e deve ser maior que zero.");
    }

    @Test
    void rejectsSupplierNameAboveEightyCharacters() {
        assertThatThrownBy(() -> Appointment.create(
                TestFixtures.ORDER_ID,
                "F".repeat(81), "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, null, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("Nome do fornecedor deve ter no máximo 80 caracteres.");
    }

    @Test
    void rejectsNotesAboveFiveHundredCharacters() {
        assertThatThrownBy(() -> Appointment.create(
                TestFixtures.ORDER_ID,
                "Fornecedor", "fornecedor@example.com", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, "N".repeat(501), TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessage("Observação deve ter no máximo 500 caracteres.");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> Appointment.create(
                TestFixtures.ORDER_ID,
                "Fornecedor", "email-invalido", "abc1d23", TestFixtures.DATE,
                TestFixtures.timeSlot(), TestFixtures.truckType(), TestFixtures.palletized(),
                10, null, TestFixtures.user(), TestFixtures.NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("E-mail inválido");
    }
}
