package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.PalletizationCode;
import com.redekrill.scheduling.domain.model.PalletizationType;

import java.util.List;
import java.util.Optional;

public interface PalletizationTypeRepository {

    List<PalletizationType> findAll();

    Optional<PalletizationType> findById(Long id);

    Optional<PalletizationType> findByCode(PalletizationCode code);
}
