/*
 * Uma página de resultados, sem depender de nenhuma classe do Spring Data.
 *
 * Os repositórios de domain devolvem isto, e não Page<T>, para que a camada de
 * aplicação e a de apresentação continuem ignorando como os dados são buscados.
 * O adaptador JPA é quem converte a Page do framework em PageResult, na borda de
 * fora. Se Page aparecesse num caso de uso, trocar de persistência viraria
 * refatoração em cascata.
 *
 * Página é contada a partir de zero, igual ao PageRequest. O tamanho pedido já
 * chega ajustado pela aplicação, na classe Pagination. As conferências daqui,
 * então, não são regra de negócio: pegam quem montou a página do jeito errado.
 * Por isso lançam IllegalArgumentException e não DomainException — o usuário não
 * tem como ver essa mensagem, quem precisa ler é o desenvolvedor.
 */
package com.redekrill.scheduling.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Subconjunto ordenado de resultados junto com a informação de paginação
 * necessária para a tela montar os controles de página.
 *
 * O que esta classe faz: congela a lista recebida e confere página, tamanho e
 * totais.
 *
 * O que ela NÃO faz: cortar ou ordenar nada. Quem escolhe o conteúdo é o
 * repositório; aqui só se transporta o recorte já feito.
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        // List.copyOf barra conteúdo nulo e impede que a lista mude depois de entregue.
        content = List.copyOf(Objects.requireNonNull(content, "Conteúdo é obrigatório."));
        if (page < 0) {
            throw new IllegalArgumentException("Página não pode ser negativa.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Tamanho da página deve ser maior que zero.");
        }
        if (totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Totais da página não podem ser negativos.");
        }
    }

    /**
     * Atalho usado pelos adaptadores de persistência ao converter a Page do
     * Spring Data.
     *
     * @param content       itens da página; não pode ser nulo.
     * @param page          índice da página, a partir de zero.
     * @param size          quantidade de itens por página.
     * @param totalElements itens existentes em todo o filtro.
     * @param totalPages    páginas necessárias para percorrer tudo.
     * @return a página já validada e com lista imutável.
     */
    public static <T> PageResult<T> of(List<T> content, int page, int size,
                                       long totalElements, int totalPages) {
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }
}
