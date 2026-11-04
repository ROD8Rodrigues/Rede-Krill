/*
 * O registro procurado não está lá — ou não está ao alcance de quem pediu.
 *
 * Vira 404. Ter um tipo único para todos os "não achei" também evita revelar se
 * o recurso não existe ou se simplesmente não é visível para aquele usuário.
 */
package com.redekrill.scheduling.domain.exception;

/**
 * Recurso não encontrado. Vira 404 na resposta HTTP.
 */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
