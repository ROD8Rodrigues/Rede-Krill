/*
 * Catálogo de paletização no banco, o CRUD completo de um cadastro pequeno.
 *
 * A ordem por id é pedida ao Spring Data pelo nome do método, e o efeito prático é o
 * que o usuário vê: os dois tipos que vêm do script de referência aparecem primeiro no
 * seletor, na ordem em que foram criados.
 *
 * Os dois exists são o que impede nomes duplicados — um no cadastro, outro na edição,
 * onde a comparação precisa excluir o próprio registro. save devolve o modelo lido de
 * volta da entity salva, e deleteById é físico: a proteção contra excluir tipo em uso é
 * do caso de uso, não do banco.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.PalletizationType;
import com.redekrill.scheduling.domain.repository.PalletizationTypeRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.PalletizationTypePersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.PalletizationTypeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * Implementação de PalletizationTypeRepository sobre Spring Data JPA.
 */
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
    public PalletizationType save(PalletizationType type) {
        return PalletizationTypePersistenceMapper.toDomain(
                repository.save(PalletizationTypePersistenceMapper.toEntity(type)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, Long id) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, id);
    }
}
