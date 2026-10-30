package com.redekrill.scheduling.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    @Test
    void normalizesPageAndSize() {
        assertThat(Pagination.page(-3)).isZero();
        assertThat(Pagination.page(4)).isEqualTo(4);
        assertThat(Pagination.size(0)).isEqualTo(1);
        assertThat(Pagination.size(25)).isEqualTo(25);
        assertThat(Pagination.size(1_000)).isEqualTo(Pagination.MAX_PAGE_SIZE);
    }
}
