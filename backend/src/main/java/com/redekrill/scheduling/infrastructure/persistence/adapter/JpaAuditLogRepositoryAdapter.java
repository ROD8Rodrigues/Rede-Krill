/*
 * Auditoria no banco: uma escrita e uma leitura paginada, nada além.
 *
 * Não há update nem delete neste adaptador, e isso é a fronteira da trilha — o que
 * foi gravado não se reescreve. A ordem (createdAt desc, id desc) é aplicada aqui, no
 * PageRequest, porque o caso de uso de listagem não conhece Pageable nem Sort.
 *
 * save devolve o modelo com o id gerado. A entity salva não é mapeada de volta: o log
 * já estava completo antes de ir, e não há coluna calculada pelo banco.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.AuditLog;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AuditLogRepository;
import com.redekrill.scheduling.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.mapper.AuditLogPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
/**
 * Implementação de AuditLogRepository sobre Spring Data JPA.
 */
public class JpaAuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repository;

    public JpaAuditLogRepositoryAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity saved = repository.save(AuditLogPersistenceMapper.toEntity(auditLog));
        return auditLog.withId(saved.getId());
    }

    @Override
    public PageResult<AuditLog> findAll(int page, int size) {
        Page<AuditLogJpaEntity> result = repository.findAll(PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        ));

        return PageResult.of(
                result.getContent().stream().map(AuditLogPersistenceMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}
