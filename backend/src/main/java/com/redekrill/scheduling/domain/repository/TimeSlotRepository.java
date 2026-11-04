/*
 * Contrato das faixas horárias de operação.
 *
 * A tabela é pequena e fixa no dia a dia: quatro faixas de duas horas, criadas no
 * seed, raramente alteradas. Por isso findAll devolve a lista inteira, sem
 * paginação — e é assim que a sobreposição é conferida. Quando alguém quer criar a
 * faixa das 10:00 às 12:00, a pergunta é "alguma das que já existem invade esse
 * intervalo?", e responder isso com paginação seria um absurdo. O mesmo findAll
 * atende a tela de ocupação, que precisa de todas as faixas do dia para desenhar
 * as barras.
 *
 * Não existe método de busca por nome porque faixa não tem nome: ela é o próprio
 * intervalo, e a unicidade se dá por sobreposição, conferida no caso de uso com a
 * regra que mora em TimeSlot.overlaps.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.TimeSlot;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e manutenção das janelas de duas horas.
 *
 * O que esta interface faz: entrega a lista completa, um id, grava e apaga.
 *
 * O que ela NÃO faz: conferir sobreposição nem recusar exclusão de faixa em uso —
 * as duas decisões são de caso de uso, uma chamando TimeSlot.overlaps e a outra
 * chamando AppointmentRepository.existsByTimeSlotId.
 */
public interface TimeSlotRepository {

    /**
     * Todas as faixas. O adaptador entrega ordenado pela hora de início, o que é o
     * que faz sentido para o operador ler a sequência do dia.
     */
    List<TimeSlot> findAll();

    Optional<TimeSlot> findById(Long id);

    TimeSlot save(TimeSlot timeSlot);

    void deleteById(Long id);
}
