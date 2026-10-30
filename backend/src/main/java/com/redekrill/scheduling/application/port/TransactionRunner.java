package com.redekrill.scheduling.application.port;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T execute(Supplier<T> action);
}
