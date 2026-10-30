package com.redekrill.scheduling.domain.model;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
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

    public static <T> PageResult<T> of(List<T> content, int page, int size,
                                       long totalElements, int totalPages) {
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }
}
