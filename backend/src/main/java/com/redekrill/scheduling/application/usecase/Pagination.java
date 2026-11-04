/*
 * A régua de paginação, escrita uma vez para todos os casos de uso de lista.
 *
 * Existe porque o controller não pode ser a única barreira: se size viesse direto do
 * request para PageRequest, um ?size=1000000 viraria uma consulta de um milhão de
 * linhas. Aqui o valor é apertado antes de tocar o banco — tamanho zero ou negativo
 * sobe para um, e o teto é MAX_PAGE_SIZE. Página negativa vira a primeira.
 *
 * Page não tem teto de propósito: faz parte do contrato deixar o cliente navegar até
 * o fim do resultado, e quem passa da última página recebe lista vazia.
 *
 * É classe final com construtor privado porque não há estado: são duas funções de
 * ajuste, e nada aqui deveria ser instanciado ou herdado.
 */
package com.redekrill.scheduling.application.usecase;

/**
 * Normalização de página e tamanho, aplicada antes da chamada ao repositório.
 */
public final class Pagination {

    public static final int MAX_PAGE_SIZE = 100;

    private Pagination() {
    }

    public static int page(int page) {
        return Math.max(page, 0);
    }

    public static int size(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
