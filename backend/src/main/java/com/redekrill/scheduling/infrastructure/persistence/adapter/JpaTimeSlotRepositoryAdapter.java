/*
 * Faixas horárias no banco, e a decisão de onde mora a ordenação.
 *
 * O caso de uso que verifica sobreposição lê todas as faixas e compara no modelo. Não
 * há consulta de intervalo aqui porque a regra de conflito (encostar não é invadir) é
 * de TimeSlot.overlaps, e duplicá-la em JPQL seria ter duas respostas possíveis para a
 * mesma pergunta.
 *
 * A ordenação cronológica vem do nome do método — findAllByOrderByStartTimeAsc — e é
 * ela que põe o seletor da tela na ordem esperada.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.TimeSlot;
import com.redekrill.scheduling.domain.repository.TimeSlotRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.TimeSlotPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.TimeSlotJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * Implementação de TimeSlotRepository sobre Spring Data JPA.
 */
public class JpaTimeSlotRepositoryAdapter implements TimeSlotRepository {

    private final TimeSlotJpaRepository repository;

    public JpaTimeSlotRepositoryAdapter(TimeSlotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TimeSlot> findAll() {
        return repository.findAllByOrderByStartTimeAsc().stream()
                .map(TimeSlotPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TimeSlot> findById(Long id) {
        return repository.findById(id).map(TimeSlotPersistenceMapper::toDomain);
    }

    @Override
    public TimeSlot save(TimeSlot timeSlot) {
        return TimeSlotPersistenceMapper.toDomain(
                repository.save(TimeSlotPersistenceMapper.toEntity(timeSlot)));
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
