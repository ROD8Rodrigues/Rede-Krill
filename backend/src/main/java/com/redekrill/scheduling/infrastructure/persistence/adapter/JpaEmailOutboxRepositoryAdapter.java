/*
 * Fila de confirmações no banco, com o lote sob trava.
 *
 * findDue é o método que dá trabalho, e ele tem duas proteções herdadas da consulta: o
 * lock pessimista, que impede que duas varreduras peguem a mesma mensagem, e a
 * ordenação por criação, para a mensagem mais antiga não ficar atrás de novidades.
 *
 * O limit vem de PageRequest.of(0, Math.max(1, limit)). O Math.max existe porque
 * PageRequest recusa tamanho negativo e página de tamanho zero não devolve nada: uma
 * propriedade de lote mal configurada derrubaria a varredura inteira, em vez de apenas
 * não processar mensagem alguma.
 *
 * Nenhum método apaga linha. Mensagem enviada, falhada ou cancelada continua na tabela
 * como histórico do aviso.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.EmailOutboxMessage;
import com.redekrill.scheduling.domain.repository.EmailOutboxRepository;
import com.redekrill.scheduling.infrastructure.persistence.mapper.EmailOutboxPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.EmailOutboxJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
/**
 * Implementação de EmailOutboxRepository sobre Spring Data JPA.
 */
public class JpaEmailOutboxRepositoryAdapter implements EmailOutboxRepository {

    private final EmailOutboxJpaRepository repository;

    public JpaEmailOutboxRepositoryAdapter(EmailOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmailOutboxMessage save(EmailOutboxMessage message) {
        return EmailOutboxPersistenceMapper.toDomain(
                repository.save(EmailOutboxPersistenceMapper.toEntity(message)));
    }

    @Override
    public Optional<EmailOutboxMessage> findById(Long id) {
        return repository.findById(id).map(EmailOutboxPersistenceMapper::toDomain);
    }

    @Override
    public List<EmailOutboxMessage> findDue(Instant now, int limit) {
        return repository.findDue(now, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(EmailOutboxPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<EmailOutboxMessage> findPendingByAppointmentId(Long appointmentId) {
        return repository.findPendingByAppointmentId(appointmentId).stream()
                .map(EmailOutboxPersistenceMapper::toDomain)
                .toList();
    }
}
