/*
 * Contrato dos tipos de caminhão, o cadastro mais simples do sistema.
 *
 * É a única tabela de referência que pode ser esvaziada: não há regra de
 * obrigatoriedade prendendo Carreta, Truck ou VUC ao cadastro. O que segura a
 * exclusão é uso — um tipo com agendamento vinculado não sai — e essa checagem não
 * está aqui, está em AppointmentRepository.existsByTruckTypeId. O caso de uso é
 * quem consulta as duas coisas na ordem certa.
 *
 * Uma nota honesta sobre existsByName: o nome sugere comparação exata, e a
 * implementação é sem diferenciar maiúsculas. Não é engano de quem escreveu o
 * adaptador nem promessa vazia do contrato — o efeito contratado é "não deixe
 * dois cadastros com o mesmo nome", e "mesmo" ali ignora caixa. Se algum dia a
 * comparação exata for o que se quer, a mudança é no adaptador, e o contrato fica.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.TruckType;

import java.util.List;
import java.util.Optional;

/**
 * Leitura e manutenção dos tipos de veículo oferecidos no formulário.
 *
 * O que esta interface faz: lista, busca por id, grava, apaga e confere nome
 * repetido.
 *
 * O que ela NÃO faz: ordenar por capacidade nem nada parecido — tamanho de
 * caminhão não entra no cálculo de pallets. A lista sai ordenada por nome, e isso
 * é escolha do adaptador, não regra de domínio.
 */
public interface TruckTypeRepository {

    List<TruckType> findAll();

    Optional<TruckType> findById(Long id);

    TruckType save(TruckType truckType);

    void deleteById(Long id);

    /**
     * Repetição de nome no cadastro; vale para criação.
     */
    boolean existsByName(String name);

    /**
     * A mesma checagem na edição, em que o registro comparado é o próprio.
     */
    boolean existsByNameAndIdNot(String name, Long id);
}
