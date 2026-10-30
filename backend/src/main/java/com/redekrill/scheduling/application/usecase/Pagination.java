package com.redekrill.scheduling.application.usecase;

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
