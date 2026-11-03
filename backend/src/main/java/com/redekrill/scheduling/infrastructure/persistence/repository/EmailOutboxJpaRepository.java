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

public interface EmailOutboxJpaRepository extends JpaRepository<EmailOutboxJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message from EmailOutboxJpaEntity message
            where message.status = com.redekrill.scheduling.domain.model.EmailOutboxStatus.PENDING
              and message.nextAttemptAt <= :now
            order by message.createdAt asc
            """)
    List<EmailOutboxJpaEntity> findDue(@Param("now") Instant now, Pageable pageable);
}
