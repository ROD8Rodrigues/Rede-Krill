package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.PalletizationTypePersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.PalletizationTypeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaPalletizationTypeRepositoryAdapter implements PalletizationTypeRepository {

    private final PalletizationTypeJpaRepository repository;

    public JpaPalletizationTypeRepositoryAdapter(PalletizationTypeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PalletizationType> findAll() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(PalletizationTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PalletizationType> findById(Long id) {
        return repository.findById(id).map(PalletizationTypePersistenceMapper::toDomain);
    }

    @Override
    public Optional<PalletizationType> findByCode(PalletizationCode code) {
        return repository.findByCode(code).map(PalletizationTypePersistenceMapper::toDomain);
    }
}
