/*
 * Consultas do cadastro de paletização, todas por nome de método.
 *
 * Sem @Query aqui: ordenação por id e as duas checagens de nome em minúsculas são o que o
 * Spring Data deriva do nome, e o custo de escrever JPQL não compra nada. A ordenação por
 * id é a ordem de criação, então os dois tipos que vêm do script de referência aparecem
 * primeiro no seletor.
 *
 * A variante AndIdNot existe para a edição, onde o nome do próprio registro não pode brigar
 * com ele mesmo.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório Spring Data da entidade de tipo de paletização.
 */
public interface PalletizationTypeJpaRepository extends JpaRepository<PalletizationTypeJpaEntity, Long> {

    List<PalletizationTypeJpaEntity> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
