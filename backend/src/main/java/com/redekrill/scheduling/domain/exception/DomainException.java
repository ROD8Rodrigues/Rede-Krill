/*
 * Raiz das exceções do negócio.
 *
 * Toda regra de domínio que precisa dizer "isso não dá para continuar" lança
 * algo que herde daqui. Assim a camada de apresentação consegue tratar uma
 * família só, em vez de conhecer cada situação individualmente.
 *
 * É RuntimeException de propósito: violar uma regra não é um erro que o
 * chamador precise capturar em cada linha. Ela sobe até o handler global,
 * que traduz para a resposta HTTP adequada.
 */
package com.redekrill.scheduling.domain.exception;

/**
 * Exceção base do domínio. Usada direto quando a situação não merece um tipo próprio.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
