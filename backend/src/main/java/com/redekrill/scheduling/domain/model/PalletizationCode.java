/*
 * Os códigos que distinguem um tipo de paletização de outro.
 *
 * Os três valores têm funções diferentes. PALETIZADO e NAO_PALETIZADO são os
 * tipos que a regra de capacidade conhece: vêm semeados no banco, têm
 * multiplicador preso e não podem ser excluídos. OUTRO é a saída para o ADMIN
 * cadastrar um tipo próprio, escolhendo o multiplicador dentro do intervalo que
 * o domínio aceita.
 *
 * O código é separado do nome porque o nome é texto livre e muda, enquanto o
 * código é o que o domínio consulta para saber se o tipo é um dos fixos.
 */
package com.redekrill.scheduling.domain.model;

public enum PalletizationCode {
    // Carga já montada em pallets: conta 1 para 1 na faixa horária.
    PALETIZADO,
    // Carga solta: conta em dobro na faixa horária, mas não no total do dia.
    NAO_PALETIZADO,
    // Tipo inventado pelo ADMIN; o multiplicador é definido no cadastro.
    OUTRO
}
