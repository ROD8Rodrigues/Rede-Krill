package com.redekrill.scheduling.support;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.model.Role;
import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.model.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class TestFixtures {

    public static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    public static final LocalDate DATE = LocalDate.of(2026, 2, 10);
    public static final long ORDER_ID = 1000L;

    private static final User USER = new User(
            1L, "Administrador", "admin@redekrill.com", "hashed-password",
            Role.ADMIN, true, NOW, NOW
    );

    private TestFixtures() {
    }

    public static User user() {
        return USER;
    }

    public static User inactiveUser() {
        return new User(1L, "Administrador", "admin@redekrill.com", "hashed-password",
                Role.ADMIN, false, NOW, NOW);
    }

    public static User userWithId(Long id) {
        return new User(id, "Administrador", "admin@redekrill.com", "hashed-password",
                Role.ADMIN, true, NOW, NOW);
    }

    public static TimeSlot timeSlot() {
        return new TimeSlot(1L, LocalTime.of(8, 0), LocalTime.of(10, 0));
    }

    public static TimeSlot otherTimeSlot() {
        return new TimeSlot(2L, LocalTime.of(13, 0), LocalTime.of(15, 0));
    }

    public static TruckType truckType() {
        return new TruckType(1L, "Carreta", "Caminhão do tipo carreta", NOW, NOW);
    }

    public static PalletizationType palletized() {
        return new PalletizationType(1L, PalletizationCode.PALETIZADO, "Paletizado", 1);
    }

    public static PalletizationType nonPalletized() {
        return new PalletizationType(2L, PalletizationCode.NAO_PALETIZADO, "Não Paletizado", 2);
    }

    public static Appointment appointment() {
        return appointment(1L, 10, palletized());
    }

    public static Appointment appointment(Long id, int palletQuantity, PalletizationType palletizationType) {
        return Appointment.create(
                ORDER_ID + id,
                "Fornecedor Krill",
                "fornecedor@example.com",
                "ABC1D23",
                DATE,
                timeSlot(),
                truckType(),
                palletizationType,
                palletQuantity,
                "Carga refrigerada",
                USER,
                NOW
        ).withId(id);
    }

    public static Appointment canceledAppointment() {
        return appointment().cancel(USER, NOW.plusSeconds(120));
    }
}
