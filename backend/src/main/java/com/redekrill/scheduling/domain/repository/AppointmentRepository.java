/*
 * O contrato de consulta e gravação de agendamentos, escrito do lado do domínio.
 *
 * Isto é uma interface sem implementação porque o domínio não sabe existir banco.
 * Quem implementa é infrastructure.persistence.adapter.JpaAppointmentRepositoryAdapter,
 * e a injeção acontece em infrastructure.config.UseCaseConfig. O efeito prático é
 * que os casos de uso chamam métodos com nomes de negócio — sumDailyPalletsByDate,
 * existsByOrderId — enquanto o SQL, a Page do Spring Data e a conversão de
 * entidade ficam do outro lado da borda.
 *
 * Nada aqui devolve tipo de framework: o recorte vem como PageResult (o record
 * deste domínio), a ausência vem como Optional, e os parâmetros são LocalDate,
 * Long e int. Uma troca de JPA por qualquer outra coisa mudaria um adaptador e
 * nenhum caso de uso.
 *
 * Os métodos se dividem em três grupos, e a divisão ajuda a ler os casos de uso:
 * grava e busca de um registro, somas e contagens para capacidade e relatório, e
 * os existsBy... que protegem integridade — duplicidade de pedido e exclusão de
 * um tipo de cadastro ainda em uso.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.PageResult;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Acesso a agendamentos, com as somas de capacidade e as checagens de integridade
 * que o domínio precisa.
 *
 * O que esta interface faz: descreve o que os casos de uso esperam de um
 * repositório de agendamentos.
 *
 * O que ela NÃO faz: dizer como isso é buscado. Não há JPQL, coluna, índice nem
 * ordem de chave estrangeira aqui — só o adaptador sabe.
 */
public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(Long id);

    /**
     * Lista da tela de agendamentos, no intervalo de datas, da mais recente para a
     * mais antiga, incluindo as canceladas.
     *
     * @param startDate primeira data do filtro; o chamador substitui a ausência por
     *                  uma data extrema, porque o intervalo é fechado nos dois lados.
     * @param endDate   última data do filtro; mesma regra de startDate.
     * @param page      índice da página, a partir de zero.
     * @param size      itens por página.
     * @return a página de agendamentos do intervalo.
     */
    PageResult<Appointment> search(LocalDate startDate, LocalDate endDate, int page, int size);

    /**
     * Lista do relatório, no intervalo de datas, da mais antiga para a mais
     * recente, com o filtro de situação que a listagem comum não oferece.
     *
     * @param startDate primeira data do intervalo.
     * @param endDate   última data do intervalo.
     * @param status    situação desejada; null traz ativas e canceladas juntas.
     * @param page      índice da página, a partir de zero.
     * @param size      itens por página.
     * @return a página de agendamentos do filtro.
     */
    PageResult<Appointment> searchReport(LocalDate startDate, LocalDate endDate,
                                         AppointmentStatus status, int page, int size);

    // Contagens do relatório: as quatro abaixo alimentam os cartões de resumo do período.
    long countByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Total de pallets declarados no período, sem multiplicador — o número do dia.
     */
    long sumPalletsByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Total de pallets considerados no período, com multiplicador — o número da faixa.
     */
    long sumEffectivePalletsByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Quantos agendamentos do período foram cancelados; entram na conta, mas não
     * ocupam capacidade.
     */
    long countCanceledByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Soma a quantidade declarada (sem multiplicador) dos agendamentos ativos do dia.
     */
    long sumDailyPalletsByDate(LocalDate date);

    /**
     * Igual à anterior, ignorando um agendamento. Serve à edição: o registro que
     * está sendo mexido já está no banco, e contá-lo de novo somaria a quantidade
     * antiga junto com a nova.
     */
    long sumDailyPalletsByDateExcluding(LocalDate date, Long appointmentId);

    /**
     * Soma a quantidade considerada na faixa (com multiplicador) dos agendamentos ativos.
     */
    long sumSlotPalletsByDateAndTimeSlot(LocalDate date, Long timeSlotId);

    /**
     * Soma a quantidade considerada na faixa, excluindo um agendamento. Mesma razão
     * da variante diária: vale para o editado, não para o total.
     */
    long sumSlotPalletsByDateAndTimeSlotExcluding(LocalDate date, Long timeSlotId, Long appointmentId);

    /**
     * Trava de duplicidade: o mesmo ID de pedido não pode ser agendado duas vezes.
     */
    boolean existsByOrderId(Long orderId);

    /**
     * A mesma checagem na edição, em que o registro comparado é o próprio.
     */
    boolean existsByOrderIdAndIdNot(Long orderId, Long appointmentId);

    /**
     * As três abaixo impedem excluir um tipo de cadastro que ainda sustenta um
     * agendamento — sem elas, a exclusão deixaria registros apontando para nada.
     */
    boolean existsByTruckTypeId(Long truckTypeId);

    boolean existsByTimeSlotId(Long timeSlotId);

    boolean existsByPalletizationTypeId(Long palletizationTypeId);
}
