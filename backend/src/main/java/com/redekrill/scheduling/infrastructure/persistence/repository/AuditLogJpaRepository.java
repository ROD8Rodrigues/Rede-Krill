/*
 * Repositório de auditoria: nem um método próprio.
 *
 * Herdar JpaRepository dá o save e o findAll paginado que o adaptador usa, e a ordenação é
 * pedida por Sort no PageRequest — não por consulta aqui. O silêncio é a regra: log não
 * tem busca por texto, atualização nem remoção. Se algum dia alguém adicionar um delete, a
 * pergunta é qual requisito de segurança pediu isso.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório Spring Data da entidade de auditoria.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, Long> {
}
