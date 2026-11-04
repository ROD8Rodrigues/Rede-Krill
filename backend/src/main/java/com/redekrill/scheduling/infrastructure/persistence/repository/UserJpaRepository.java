/*
 * Consultas de usuário, todas por e-mail sem diferenciar maiúsculas.
 *
 * São três: achar, saber se existe e saber se existe outro com o mesmo e-mail. As duas de
 * existência sustentam o cadastro e a edição, e a de busca é o caminho do login — o
 * LoginUseCase normaliza o texto para minúsculas antes de chamar, mas a coluna guarda o que
 * foi digitado, então a comparação tolerante continua sendo o que fecha as duas pontas.
 *
 * Não há consulta por papel nem por atividade: a listagem de administração é paginada e
 * ordenada por nome no adaptador, e a tela ordena o que mostra.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório Spring Data da entidade de usuário.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
