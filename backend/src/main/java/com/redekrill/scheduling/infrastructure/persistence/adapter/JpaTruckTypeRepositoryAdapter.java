package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.TruckType;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;
import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.mapper.TruckTypePersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.TruckTypeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaTruckTypeRepositoryAdapter implements TruckTypeRepository {

    private final TruckTypeJpaRepository repository;

    public JpaTruckTypeRepositoryAdapter(TruckTypeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TruckType> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(TruckTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TruckType> findById(Long id) {
        return repository.findById(id).map(TruckTypePersistenceMapper::toDomain);
    }

    @Override
    public TruckType save(TruckType truckType) {
        TruckTypeJpaEntity saved = repository.save(TruckTypePersistenceMapper.toEntity(truckType));
        return truckType.withId(saved.getId());
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, id);
    }
}
