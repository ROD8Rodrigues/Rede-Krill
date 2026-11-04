/*
 * O envelope de paginação que toda lista devolve.
 *
 * Quatro números e o conteúdo. size é o tamanho da página pedida, já ajustado pela
 * aplicação (mínimo 1, teto de 100), então o valor no corpo pode não ser o que o cliente
 * mandou — e a tela deve se guiar pelo que veio.
 *
 * O método estático from é o único jeito de construir a partir de PageResult: ele mapeia
 * cada item com a função que o controller passa e copia a lista para imutável no
 * construtor compacto. Isso mantém controller escrevendo uma linha e impede que a
 * paginação seja remontada campo a campo em sete lugares diferentes.
 *
 * totalElements é long porque contagem de tabela não deve ter teto de inteiro; página e
 * tamanho ficam em int porque são escolhas de quem consulta.
 */
package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * Projeção paginada compartilhada por todas as rotas de lista. Não conhece Spring Data: o
 * tipo de entrada é PageResult, da aplicação.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <S, T> PageResponse<T> from(PageResult<S> source, Function<S, T> mapper) {
        return new PageResponse<>(
                source.content().stream().map(mapper).toList(),
                source.page(),
                source.size(),
                source.totalElements(),
                source.totalPages()
        );
    }
}
