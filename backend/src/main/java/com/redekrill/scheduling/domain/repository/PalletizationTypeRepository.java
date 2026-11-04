/*
 * Contrato dos tipos de paletização.
 *
 * A forma é igual à dos outros cadastros de referência, e o que muda é o peso:
 * estes registros definem multiplicador de capacidade. Não basta existir — os dois
 * tipos da regra precisam continuar existindo, e é por isso que a exclusão passa
 * antes por PalletizationType.mandatory do que por qualquer consulta desta
 * interface.
 *
 * O adaptador entrega a lista ordenada pelo id, o que mantém Paletizado e Não
 * Paletizado nas duas primeiras posições, na ordem em que o seed os criou. Para o
 * formulário, é a ordem que faz a dupla fixa aparecer antes das inventadas pelo
 * ADMIN.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.PalletizationType;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e manutenção dos tipos de paletização e de seus multiplicadores.
 *
 * O que esta interface faz: lista, busca por id, grava, apaga e confere nome
 * repetido.
 *
 * O que ela NÃO faz: proteger os tipos fixos nem validar multiplicador. As duas
 * regras são de domínio — a primeira vive em PalletizationType.mandatory, a
 * segunda no construtor compacto do mesmo registro.
 */
public interface PalletizationTypeRepository {

    List<PalletizationType> findAll();

    Optional<PalletizationType> findById(Long id);

    PalletizationType save(PalletizationType type);

    void deleteById(Long id);

    /**
     * Repetição de nome no cadastro; vale para criação.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * A mesma checagem na edição, em que o registro comparado é o próprio.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
