/*
 * Consultas do cadastro de caminhão: ordenação e unicidade de nome.
 *
 * Os dois exists são em minúsculas (IgnoreCase), e é por eles que o nome do port
 * application.domain.TruckTypeRepository — existsByName — chega aqui sem esse sufixo: o
 * comportamento desejado é não diferenciar maiúsculas, e o detalhe de como isso é feito
 * pertence à persistência.
 *
 * findAllByOrderByNameAsc dá o catálogo em ordem alfabética, que é como a tela lista.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório Spring Data da entidade de tipo de caminhão.
 */
public interface TruckTypeJpaRepository extends JpaRepository<TruckTypeJpaEntity, Long> {

    List<TruckTypeJpaEntity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
