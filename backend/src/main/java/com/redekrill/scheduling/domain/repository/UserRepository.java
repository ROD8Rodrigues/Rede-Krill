/*
 * Contrato de acesso às contas de usuário.
 *
 * É a interface mais enxuta deste pacote porque conta é pouco consultada: se
 * busca por id ou por e-mail, grava, e lista para a tela administrativa. Não há
 * delete — desativar é o caminho (o campo active em User), já que o histórico de
 * auditoria referencia o e-mail da pessoa.
 *
 * Os dois métodos por e-mail parecem sensíveis a maiúscula, e o nome não resolve
 * isso sozinho: o modelo já normaliza o endereço para minúscula na validação, e o
 * adaptador de persistência ainda procura ignorando caixa. O resultado é que
 * "Ana@RedeKrill.com" e "ana@redekrill.com" chegam na mesma conta — o que importa
 * aqui é o comportamento contratado, não o nome do método.
 */
package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.User;

import java.util.Optional;

/**
 * Busca, gravação e listagem de contas de usuário.
 *
 * O que esta interface faz: descreve as consultas que a autenticação e a
 * administração de contas precisam.
 *
 * O que ela NÃO faz: prometer ordem de listagem — isso é decisão do adaptador,
 * hoje ordenado por nome — nem regra de unicidade, que fica nos casos de uso.
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    PageResult<User> findAll(int page, int size);

    User save(User user);

    /**
     * Usado antes de criar, para recusar a segunda conta com o mesmo endereço.
     */
    boolean existsByEmail(String email);

    /**
     * A mesma checagem na edição, em que o registro comparado é o próprio.
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}
