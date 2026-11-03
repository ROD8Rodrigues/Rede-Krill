package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PalletizationTypeJpaRepository extends JpaRepository<PalletizationTypeJpaEntity, Long> {

    List<PalletizationTypeJpaEntity> findAllByOrderByIdAsc();

    Optional<PalletizationTypeJpaEntity> findByCode(PalletizationCode code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
