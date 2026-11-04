/*
 * Um tipo de paletização como ele é devolvido.
 *
 * O campo que parece redundante — code — é o que permite à tela decidir sozinha se o
 * multiplicador pode ser editado. Com o código em mãos, o formulário sabe que
 * PALETIZADO e NAO_PALETIZADO vêm travados e só OUTRO aceita número livre. Sem ele,
 * restaria comparar pelo nome, que é texto que o usuário pode mudar.
 *
 * Diferente de TruckTypeResult, não leva datas de criação e atualização: a paletização
 * é uma tabela de regras curtas, e a auditoria é quem conta a história dela.
 */
package com.redekrill.scheduling.application.dto.reference;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;

/**
 * Projeção de leitura de PalletizationType.
 */
public record PalletizationTypeResult(
        Long id,
        PalletizationCode code,
        String name,
        int capacityMultiplier
) {

    public static PalletizationTypeResult from(PalletizationType type) {
        return new PalletizationTypeResult(
                type.id(),
                type.code(),
                type.name(),
                type.capacityMultiplier()
        );
    }
}
