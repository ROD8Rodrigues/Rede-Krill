/*
 * O padrão outbox em uma linha: a intenção de avisar, gravada junto do que ela
 * descreve.
 *
 * appointment_id é número, não relação — a fila acompanha o agendamento sem depender de
 * carregá-lo. O processador que varre a tabela precisa de status, next_attempt_at e
 * attempts, e nada mais.
 *
 * As quatro colunas de reenvio contam a história sozinhas: attempts soma as
 * tentativas, next_attempt_at é o portão do backoff, last_error guarda o motivo da última
 * falha (aparado em mil caracteres, porque exceção de SMTP pode ser enorme) e sent_at marca
 * a hora do sucesso.
 *
 * warnings é TEXT e nunca é nulo: a lista de avisos de capacidade vai unida por quebra de
 * linha, e uma mensagem sem aviso é texto vazio, não ausência.
 */
package com.redekrill.scheduling.infrastructure.persistence.entity;

import com.redekrill.scheduling.domain.model.EmailOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "email_outbox")
@Getter
@Setter
@NoArgsConstructor
/**
 * Mapeamento JPA da tabela email_outbox.
 */
public class EmailOutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(nullable = false, length = 160)
    private String recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String warnings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailOutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
