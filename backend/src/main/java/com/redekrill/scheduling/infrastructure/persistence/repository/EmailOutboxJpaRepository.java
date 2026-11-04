/*
 * As duas consultas da fila de confirmações, e a trava que faz uma varredura ser
 * segura.
 *
 * findDue usa @Lock(PESSIMISTIC_WRITE), que vira SELECT ... FOR UPDATE. Sem ele, duas
 * varreduras concorrentes — duas instâncias da aplicação, por exemplo — pegariam a mesma
 * mensagem PENDING e enviariam a mesma confirmação duas vezes. O lote vem ordenado
 * por criação, então a mensagem mais antiga não fica esperando enquanto chegam novas.
 *
 * O status é comparado com o nome completo do enum dentro do JPQL. Não é preciosismo:
 * parâmetro enumerado em consulta de comparação literal exige o nome qualificado para o
 * parser resolver o tipo.
 *
 * findPendingByAppointmentId não trava nada — é leitura do cancelamento, que só quer saber
 * o que ainda não saiu.
 */
package com.redekrill.scheduling.infrastructure.persistence.repository;

import com.redekrill.scheduling.infrastructure.persistence.entity.EmailOutboxJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repositório Spring Data da fila de e-mails.
 */
public interface EmailOutboxJpaRepository extends JpaRepository<EmailOutboxJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message from EmailOutboxJpaEntity message
            where message.status = com.redekrill.scheduling.domain.model.EmailOutboxStatus.PENDING
              and message.nextAttemptAt <= :now
            order by message.createdAt asc
            """)
    List<EmailOutboxJpaEntity> findDue(@Param("now") Instant now, Pageable pageable);

    @Query("""
            select message from EmailOutboxJpaEntity message
            where message.appointmentId = :appointmentId
              and message.status = com.redekrill.scheduling.domain.model.EmailOutboxStatus.PENDING
            order by message.createdAt asc
            """)
    List<EmailOutboxJpaEntity> findPendingByAppointmentId(@Param("appointmentId") Long appointmentId);
}
