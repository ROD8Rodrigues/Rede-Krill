/*
 * Uma janela de duas horas em que o caminhão pode chegar.
 *
 * A duração é fixa por regra do teste técnico, e ela é validada aqui em vez de
 * ficar solta no cadastro: se alguém criasse uma faixa de 45 minutos, o limite de
 * 120 pallets por faixa perderia o sentido, porque deixaria de representar a
 * mesma ocupação.
 *
 * A comparação com outra faixa é meio aberta: duas janelas que se encostam — uma
 * terminando exatamente quando a outra começa — não colidem. Conflito só existe
 * quando elas dividem algum instante.
 *
 * Camada: domain. Não conhece JPA nem HTTP, e as horas são locais, sem fuso.
 */
package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Faixa horária de operação da doca: início, fim e a regra de sobreposição.
 *
 * O que esta classe faz: garante que a janela tem exatamente duas horas e que o
 * início vem antes do fim, e responde se ela divide tempo com outra faixa.
 *
 * O que ela NÃO faz: decidir se o agendamento cabe na janela. Quem soma os
 * pallets e compara com o limite é
 * {@link com.redekrill.scheduling.domain.policy.CapacityPolicy}; a busca pelas
 * faixas já cadastradas fica nos casos de uso de referência.
 */
public record TimeSlot(
        Long id,
        LocalTime startTime,
        LocalTime endTime
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public TimeSlot {
        Objects.requireNonNull(startTime, "Horário inicial é obrigatório.");
        Objects.requireNonNull(endTime, "Horário final é obrigatório.");
        if (!startTime.isBefore(endTime)) {
            throw new DomainException("Horário inicial deve ser anterior ao horário final.");
        }
        if (!Duration.between(startTime, endTime).equals(Duration.ofHours(2))) {
            throw new DomainException("Cada faixa horária deve ter exatamente 2 horas.");
        }
    }

    /**
     * Testa se esta faixa divide algum instante com outra.
     *
     * Basta uma das quatro pontas estar dentro da outra janela. Encostar não é
     * invadir: 08:00-10:00 e 10:00-12:00 coexistem.
     *
     * @param other a faixa comparada; não pode ser nula.
     * @return true quando existe sobreposição real entre as duas janelas.
     */
    public boolean overlaps(TimeSlot other) {
        Objects.requireNonNull(other, "Faixa horária para comparação é obrigatória.");
        return startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    /**
     * Texto "HH:mm - HH:mm" usado no e-mail de confirmação, na resposta da API e
     * na trilha de auditoria.
     *
     * @return a faixa em formato legível, por exemplo "08:00 - 10:00".
     */
    public String label() {
        return startTime.format(TIME_FORMAT) + " - " + endTime.format(TIME_FORMAT);
    }
}
