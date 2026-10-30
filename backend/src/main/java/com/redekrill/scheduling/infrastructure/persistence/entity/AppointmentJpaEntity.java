package com.redekrill.scheduling.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name", nullable = false, length = 160)
    private String supplierName;

    @Column(name = "supplier_email", nullable = false, length = 160)
    private String supplierEmail;

    @Column(name = "truck_plate", nullable = false, length = 20)
    private String truckPlate;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlotJpaEntity timeSlot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truck_type_id", nullable = false)
    private TruckTypeJpaEntity truckType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "palletization_type_id", nullable = false)
    private PalletizationTypeJpaEntity palletizationType;

    @Column(name = "pallet_quantity", nullable = false)
    private int palletQuantity;

    @Column(name = "effective_pallets", nullable = false)
    private int effectivePallets;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserJpaEntity createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
