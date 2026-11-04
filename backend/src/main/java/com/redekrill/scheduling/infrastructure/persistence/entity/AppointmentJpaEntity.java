/*
 * O agendamento como a tabela appointment o enxerga.
 *
 * Espelho do modelo de domínio, com as três coisas que só a persistência precisa: as
 * anotações de mapeamento, as relações preguiçosas e a coluna de versão. As relações são
 * @ManyToOne com LAZY de propósito, e quem lê agendamento compensa isso com @EntityGraph
 * nas consultas do repositório.
 *
 * Três decisões de coluna que alguém vai questionar:
 * - order_id é unique e o banco tem índice único sobre ele. O caso de uso checa antes, mas
 *   a garantia real é a constraint: duas requisições simultâneas passam pela checagem de
 *   memória e esbarram no banco na hora de gravar.
 * - status é texto ('ACTIVE' / 'CANCELED'), não número. Enumerado por posição quebraria o
 *   histórico se a ordem dos valores mudasse.
 * - effective_pallets é gravado, não derivado. Guardar o resultado do multiplicador deixa
 *   as somas de capacidade em uma coluna só; o preço é que um tipo de paletização editado
 *   depois não recalcula os agendamentos antigos. É o preço de ter a soma em uma
 *   coluna só.
 *
 * Lombok gera getter, setter e o construtor vazio que o JPA exige. Nada de regra de
 * negócio mora aqui — quem valida é o modelo Appointment.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import com.redekrill.scheduling.domain.model.AppointmentStatus;
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
import jakarta.persistence.Version;
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
/**
 * Mapeamento JPA da tabela appointment.
 */
public class AppointmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "supplier_name", nullable = false, length = 80)
    private String supplierName;

    @Column(name = "supplier_email", nullable = false, length = 80)
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

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by_id")
    private UserJpaEntity canceledBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserJpaEntity createdBy;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
