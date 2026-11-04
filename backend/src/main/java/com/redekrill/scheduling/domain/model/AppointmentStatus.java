/*
 * Situações possíveis de um agendamento.
 *
 * Só duas, e é assim de propósito: cancelar não apaga a linha, então o
 * histórico continua consultável. Não existe "pendente" nem "confirmado" —
 * quem precisar de um estado novo vai ter que justificar no domínio antes de
 * abrir espaço aqui.
 *
 * O valor guardado no banco é o nome desta enumeração (coluna status), por
 * isso os nomes não mudam sem uma migração junto.
 */
package com.redekrill.scheduling.domain.model;

public enum AppointmentStatus {
    // Entra nas somas de capacidade do dia e da faixa de horário.
    ACTIVE,
    // Sai das somas, mas permanece no banco para consulta e auditoria.
    CANCELED
}
