/*
 * Os dois números que o centro de distribuição acompanha: 480 pallets no dia, 120
 * na faixa de duas horas.
 *
 * São constantes de domínio, e não configuração. Já existiu uma tabela
 * capacity_settings com a intenção de ajuste pelo ADMIN; nenhuma linha de código
 * nunca a leu, e ela foi derrubada em V5 para não deixar uma configuração que não
 * surte efeito. A decisão e o que ela custa estão escritos em
 * docs/arquitetura/ADR-004-limites-fixos-de-capacidade.md: mudar um limite agora
 * é lançar versão, não apertar botão.
 *
 * Esta classe também é o único lugar onde os números vivem: os casos de uso somam
 * os registros ativos e chamam assess, e a rota de ocupação lê as mesmas
 * constantes e o mesmo percentual. Se a regra precisar de um terceiro limite, ele
 * entra aqui — espalhada, vira três versões da mesma frase.
 */
package com.redekrill.scheduling.domain.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Avalia os limites de capacidade de pallets.
 *
 * <p>A política é deliberadamente não bloqueante: quando um limite é excedido,
 * ela produz avisos para o usuário, mas nunca lança exceção nem impede o cadastro.</p>
 *
 * <p>O limite diário usa a quantidade declarada. O limite da faixa usa a
 * quantidade considerada, que recebe o multiplicador do tipo de paletização.</p>
 */
public final class CapacityPolicy {

    // Vêm do enunciado do desafio; veja ADR-004 antes de pensar em tirá-los daqui.
    public static final int DAILY_LIMIT = 480;
    public static final int TIME_SLOT_LIMIT = 120;

    /**
     * Soma o lançamento novo ao que já está agendado e diz o que passou do limite.
     *
     * Os parâmetros "before" chegam prontos da consulta que soma apenas os
     * agendamentos ativos — cancelado não ocupa nada. Quem chama não precisa
     * conhecer os limites: eles saem no resultado, junto com os totais.
     *
     * @param dailyPalletsBefore  quantidade declarada já somada no dia.
     * @param slotPalletsBefore   quantidade considerada já somada na faixa.
     * @param newDailyPallets     quantidade declarada do lançamento que está entrando.
     * @param newSlotPallets      quantidade considerada do mesmo lançamento.
     * @return totais, limites e os avisos formatados com o excedente; a lista fica
     *         vazia quando tudo coube.
     * @throws IllegalArgumentException se um total anterior for negativo ou se as
     *         quantidades novas não forem positivas — é uso errado do cálculo, não
     *         regra de negócio, por isso não vira DomainException.
     */
    public CapacityAssessment assess(long dailyPalletsBefore, long slotPalletsBefore,
                                     int newDailyPallets, int newSlotPallets) {
        if (dailyPalletsBefore < 0 || slotPalletsBefore < 0) {
            throw new IllegalArgumentException("Totais de pallets não podem ser negativos.");
        }
        if (newDailyPallets <= 0) {
            throw new IllegalArgumentException("Pallets do agendamento devem ser positivos.");
        }
        if (newSlotPallets <= 0) {
            throw new IllegalArgumentException("Pallets da faixa devem ser positivos.");
        }

        long dailyTotal = dailyPalletsBefore + newDailyPallets;
        long slotTotal = slotPalletsBefore + newSlotPallets;
        List<String> warnings = new ArrayList<>(2);

        if (dailyTotal > DAILY_LIMIT) {
            warnings.add("Limite diário de %d pallets excedido: %d pallets no dia (excedente de %d)."
                    .formatted(DAILY_LIMIT, dailyTotal, dailyTotal - DAILY_LIMIT));
        }
        if (slotTotal > TIME_SLOT_LIMIT) {
            warnings.add("Limite da faixa horária de %d pallets excedido: %d pallets na faixa (excedente de %d)."
                    .formatted(TIME_SLOT_LIMIT, slotTotal, slotTotal - TIME_SLOT_LIMIT));
        }

        return new CapacityAssessment(
                dailyTotal, slotTotal, DAILY_LIMIT, TIME_SLOT_LIMIT, warnings);
    }

    /**
     * Percentual de ocupação arredondado em uma casa decimal.
     *
     * O double multiplicado por dez, arredondado e dividido por dez é a forma
     * direta de fixar uma casa sem trazer formatação para dentro do cálculo — a
     * tela decide como imprimir.
     *
     * <p>Nada aqui corta em cem: passar de cem é justamente o estado que a regra
     * quer mostrar, e um aviso escondido na barra seria pior que um estouro visível.</p>
     *
     * @param pallets total já somado do dia ou da faixa.
     * @param limit   limite comparado; zero ou negativo devolve zero em vez de dividir por nada.
     * @return a ocupação com uma casa decimal, podendo ultrapassar cem.
     */
    public static double occupancyPercentage(long pallets, int limit) {
        if (limit <= 0) {
            return 0.0;
        }
        return Math.round((pallets * 100.0 / limit) * 10.0) / 10.0;
    }
}
