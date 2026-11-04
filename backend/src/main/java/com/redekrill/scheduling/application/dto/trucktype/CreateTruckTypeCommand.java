/*
 * Pedido de cadastro de um tipo de caminhão.
 *
 * Nome e descrição, sem capacidade: o que pesa na conta de pallets é a quantidade
 * declarada no agendamento e o multiplicador da paletização, não o veículo. Tipo de
 * caminhão é classificação — serve para a doca saber com quem a carga chega.
 *
 * A unicidade do nome é checada antes de gravar, sem diferenciar maiúsculas, e o ator
 * da auditoria vem em actorEmail.
 */
package com.redekrill.scheduling.application.dto.trucktype;

/**
 * Dados de entrada para criar um tipo de caminhão.
 */
public record CreateTruckTypeCommand(
        String name,
        String description,
        String actorEmail
) {
}
