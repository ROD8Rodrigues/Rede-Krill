/*
 * Como a carga é apresentada na doca e quanto ela conta para a capacidade.
 *
 * Este registro existe porque a regra de capacidade não é a mesma para todas as
 * cargas: uma entrega sem paletização ocupa mais posição no cais do que a mesma
 * quantidade em pallets fechados. Em vez de espalhar essa diferença pelos casos
 * de uso, ela vira um número dentro do próprio tipo — o multiplicador — e o resto
 * do código só multiplica. Ele pesa apenas na soma da faixa horária; a soma do
 * dia usa a quantidade declarada, sem multiplicação.
 *
 * O construtor compacto amarra os dois tipos fixos da regra aos seus
 * multiplicadores (Paletizado 1, Não Paletizado 2). Isso não é preciosismo: se
 * alguém cadastrar "Paletizado" com multiplicador 5, as somas de capacidade
 * passam a mentir sem que nada dê erro.
 */
package com.redekrill.scheduling.domain.model;

import com.redekrill.scheduling.domain.exception.DomainException;

import java.util.Objects;

/**
 * Um tipo de paletização cadastrável, com o peso que ele tem na capacidade.
 *
 * O que esta classe faz: valida a coerência entre código e multiplicador e diz se
 * o tipo é um dos fixos da regra de negócio.
 *
 * O que ela NÃO faz: somar capacidade nem comparar com limites. Isso é do
 * {@link com.redekrill.scheduling.domain.policy.CapacityPolicy}, que recebe os
 * números já multiplicados.
 */
public record PalletizationType(
        Long id,
        PalletizationCode code,
        String name,
        // Quantas posições uma unidade declarada ocupa; entra no cálculo de pallets efetivos.
        int capacityMultiplier
) {

    public PalletizationType {
        Objects.requireNonNull(code, "Código do tipo de paletização é obrigatório.");
        name = DomainValidation.requireText(name, "Nome do tipo de paletização é obrigatório.");
        // Os dois tipos da regra de negócio têm multiplicador preso ao código. Só OUTRO é livre.
        if (code == PalletizationCode.PALETIZADO && capacityMultiplier != 1) {
            throw new DomainException("O tipo Paletizado deve manter multiplicador 1.");
        }
        if (code == PalletizationCode.NAO_PALETIZADO && capacityMultiplier != 2) {
            throw new DomainException("O tipo Não Paletizado deve manter multiplicador 2.");
        }
        // O teto existe para o cadastro avulso não transformar capacidade em número absurdo.
        if (capacityMultiplier < 1 || capacityMultiplier > 100) {
            throw new DomainException("Multiplicador de capacidade deve estar entre 1 e 100.");
        }
    }

    /**
     * Diz se este tipo é um dos dois fixos da regra de capacidade.
     *
     * Os casos de uso de escrita usam o resultado para recusar exclusão e para
     * ignorar tentativa de mudar o multiplicador: o que a regra definiu não fica
     * solto no cadastro.
     *
     * @return true para Paletizado e Não Paletizado; false apenas para os criados pelo ADMIN.
     */
    public boolean mandatory() {
        return code != PalletizationCode.OUTRO;
    }
}
