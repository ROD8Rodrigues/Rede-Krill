package com.redekrill.scheduling.support;

import com.redekrill.scheduling.application.port.TransactionRunner;

import java.util.function.Supplier;

/**
 * Executa a ação imediatamente, sem transação real, para testes unitários dos casos de uso.
 */
public final class ImmediateTransactionRunner implements TransactionRunner {

    @Override
    public <T> T execute(Supplier<T> action) {
        return action.get();
    }
}
