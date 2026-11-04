/*
 * As faixas horárias em ordem de relógio.
 *
 * Lista completa, sem paginação e sem filtro por dia: uma faixa é um intervalo do dia,
 * sem vínculo com data nem com unidade, e a tela que consome isto é a de agendamento,
 * onde o seletor precisa de todas de uma vez, na sequência cronológica esperada.
 *
 * A ordenação por horário de início vem da consulta do repositório. Aqui só se troca o
 * tipo: modelo de domínio para DTO de resposta.
 */
package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.TimeSlotResult;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;

import java.util.List;

/**
 * Leitura completa das faixas horárias, ordenadas pelo início.
 */
public class ListTimeSlotsUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public ListTimeSlotsUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<TimeSlotResult> execute() {
        return timeSlotRepository.findAll().stream()
                .map(TimeSlotResult::from)
                .toList();
    }
}
