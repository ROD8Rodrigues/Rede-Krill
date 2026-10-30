package com.redekrill.scheduling.presentation.response;

import com.redekrill.scheduling.domain.model.PageResult;

import java.util.List;
import java.util.function.Function;

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
