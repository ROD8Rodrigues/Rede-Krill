/*
 * O catálogo de tipos de caminhão, em ordem alfabética.
 *
 * Sem paginação nem filtro, e é o único cadastro que devolve createdAt e updatedAt na
 * projeção — as duas datas viajam na projeção, e quem consome decide se mostram.
 * ordenação por nome é do repositório; aqui só se trocam os tipos.
 */
package com.redekrill.scheduling.application.usecase.trucktype;

import com.redekrill.scheduling.application.dto.trucktype.TruckTypeResult;
import com.redekrill.scheduling.domain.repository.TruckTypeRepository;

import java.util.List;

/**
 * Leitura completa dos tipos de caminhão.
 */
public class ListTruckTypesUseCase {

    private final TruckTypeRepository truckTypeRepository;

    public ListTruckTypesUseCase(TruckTypeRepository truckTypeRepository) {
        this.truckTypeRepository = truckTypeRepository;
    }

    public List<TruckTypeResult> execute() {
        return truckTypeRepository.findAll().stream()
                .map(TruckTypeResult::from)
                .toList();
    }
}
