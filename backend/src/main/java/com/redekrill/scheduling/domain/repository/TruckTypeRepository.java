package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.TruckType;

import java.util.List;
import java.util.Optional;

public interface TruckTypeRepository {

    List<TruckType> findAll();

    Optional<TruckType> findById(Long id);

    TruckType save(TruckType truckType);

    void deleteById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
