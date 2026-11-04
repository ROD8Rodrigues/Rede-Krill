/*
 * O catálogo de paletização inteiro, numa lista só.
 *
 * Sem paginação, e faz sentido: são os dois tipos fixos mais o que o ADMIN criar, e a
 * tela usa a lista para montar o seletor do formulário. A ordem por id é a ordem de
 * criação — quem vem do script de referência primeiro — definida no adaptador do
 * repositório, não aqui.
 *
 * Não há filtro por código nem por multiplicador, porque o consumidor da lista é um
 * dropdown.
 */
package com.redekrill.scheduling.application.usecase.reference;

import com.redekrill.scheduling.application.dto.reference.PalletizationTypeResult;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;

import java.util.List;

/**
 * Leitura completa dos tipos de paletização.
 */
public class ListPalletizationTypesUseCase {

    private final PalletizationTypeRepository palletizationTypeRepository;

    public ListPalletizationTypesUseCase(PalletizationTypeRepository palletizationTypeRepository) {
        this.palletizationTypeRepository = palletizationTypeRepository;
    }

    public List<PalletizationTypeResult> execute() {
        return palletizationTypeRepository.findAll().stream()
                .map(PalletizationTypeResult::from)
                .toList();
    }
}
