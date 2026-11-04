/*
 * Como um caso de uso pede "rode isso dentro de uma transação" sem conhecer o Spring.
 *
 * TransactionRunner recebe um Supplier e devolve o resultado; aqui o Supplier é entregue
 * ao TransactionTemplate, que abre, confirma e desfaz conforme a exceção. O ganho aparece
 * no teste: o caso de uso é exercitado com um runner que só chama o Supplier, sem subir
 * contexto nem banco.
 *
 * O escopo é o padrão do Spring (PROPAGATION_REQUIRED, sem nada configurado aqui). Os
 * métodos @Transactional dos adaptadores de persistência entram na transação que já está
 * aberta em vez de criar outra — é assim que um caso de uso garante que duas escritas
 * valem juntas ou não valem nenhuma. RuntimeException lançada dentro faz rollback, e é
 * por isso que a rotação de refresh revoga a sessão dentro da transação e só lança a
 * rejeição depois dela: a revogação precisa sobreviver ao erro que ela mesma provoca.
 */
package com.redekrill.scheduling.infrastructure.transaction;

import com.redekrill.scheduling.application.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Adapter de TransactionRunner sobre TransactionTemplate. Não escolhe propagação nem
 * isolamento: usa o padrão do gestor de transações configurado pelo Spring Boot.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
