/*
 * Contrato de "execute isto dentro de uma transação".
 *
 * É o port mais discreto do pacote e um dos que mais sustentam a arquitetura. Os
 * casos de uso precisam de transação, mas não podem conhecer @Transactional nem o
 * Spring — se conhecessem, a camada de aplicação inteira dependeria do framework e
 * cada teste de caso de uso precisaria de contexto. Aqui fica a intenção; a execução
 * é de infrastructure.transaction.SpringTransactionRunner.
 *
 * A assinatura devolve o que a ação devolver, incluindo null. Isso importa no padrão
 * usado em ChangePasswordUseCase e AuthSessionService.rotate: a transação produz um
 * resultado interno — inclusive "recusei, mas o efeito precisa sobreviver" — e o
 * lançamento da exceção acontece do lado de fora. Um runner que devolvesse void não
 * permitiria esse arranjo.
 */
package com.redekrill.scheduling.application.port;

import java.util.function.Supplier;

/**
 * Fronteira de transação sob controle do caso de uso. Implementada pela
 * infraestrutura, com Spring do lado de lá.
 */
public interface TransactionRunner {

    <T> T execute(Supplier<T> action);
}
