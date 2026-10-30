package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TruckTypeJpaRepository extends JpaRepository<TruckTypeJpaEntity, Long> {

    List<TruckTypeJpaEntity> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
