/*
 * O adaptador mais pesado do pacote, e o que mais justifica existir.
 *
 * A escrita monta uma entity nova em vez de aproveitar a que veio da leitura. Com
 * isso o modelo Appointment manda: o que vai para a tabela é exatamente o que a
 * camada de aplicação produziu, sem merge nem estado gerenciado por trás. As
 * referências entram como getReference(id), que grava a chave estrangeira sem
 * SELECT — carregar faixa, caminhão, paletização e dois usuários para um INSERT seria
 * um SELECT por relação em cada cadastro.
 *
 * save devolve appointment.withId(saved.getId()): a instância de domínio continua sendo
 * a fonte da resposta, não uma re-leitura da entity.
 *
 * nullSafe existe porque SUM sobre zero linhas devolve null no SQL, e a política de
 * capacidade trabalha com long. As somas de relatório já vêm coalesce da query; as de
 * capacidade não, e é aqui que o zero aparece.
 *
 * A leitura de um ou vários agendamentos usa as consultas com @EntityGraph do
 * repositório. Sem isso, montar o modelo de domínio tocaria cinco relações preguiçosas
 * linha a linha.
 */
package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.AppointmentStatus;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.AppointmentJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
/**
 * Implementação de AppointmentRepository sobre Spring Data JPA.
 */
public class JpaAppointmentRepositoryAdapter implements AppointmentRepository {

    private final AppointmentJpaRepository repository;
    private final EntityManager entityManager;

    public JpaAppointmentRepositoryAdapter(AppointmentJpaRepository repository,
                                           EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentJpaEntity entity = new AppointmentJpaEntity();
        entity.setId(appointment.id());
        entity.setOrderId(appointment.orderId());
        entity.setSupplierName(appointment.supplierName());
        entity.setSupplierEmail(appointment.supplierEmail());
        entity.setTruckPlate(appointment.truckPlate());
        entity.setAppointmentDate(appointment.date());
        entity.setTimeSlot(entityManager.getReference(TimeSlotJpaEntity.class, appointment.timeSlot().id()));
        entity.setTruckType(entityManager.getReference(TruckTypeJpaEntity.class, appointment.truckType().id()));
        entity.setPalletizationType(entityManager.getReference(
                PalletizationTypeJpaEntity.class, appointment.palletizationType().id()));
        entity.setPalletQuantity(appointment.palletQuantity());
        entity.setEffectivePallets(appointment.effectivePallets());
        entity.setNotes(appointment.notes());
        entity.setStatus(appointment.status());
        entity.setCanceledBy(appointment.canceledBy() == null
                ? null
                : entityManager.getReference(UserJpaEntity.class, appointment.canceledBy().id()));
        entity.setCanceledAt(appointment.canceledAt());
        entity.setCreatedBy(entityManager.getReference(UserJpaEntity.class, appointment.createdBy().id()));
        entity.setCreatedAt(appointment.createdAt());
        entity.setUpdatedAt(appointment.updatedAt());
        entity.setVersion(appointment.version());

        AppointmentJpaEntity saved = repository.save(entity);
        return appointment.withId(saved.getId());
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return repository.findDetailedById(id).map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Appointment> search(LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<AppointmentJpaEntity> result = repository.search(
                startDate, endDate, PageRequest.of(page, size));

        return PageResult.of(
                result.getContent().stream().map(AppointmentPersistenceMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public PageResult<Appointment> searchReport(LocalDate startDate, LocalDate endDate,
                                                AppointmentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AppointmentJpaEntity> result = status == null
                ? repository.searchReport(startDate, endDate, pageable)
                : repository.searchReportByStatus(startDate, endDate, status, pageable);
        return PageResult.of(
                result.getContent().stream().map(AppointmentPersistenceMapper::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public long countByPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.countByPeriod(startDate, endDate);
    }

    @Override
    public long sumPalletsByPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.sumPalletsByPeriod(startDate, endDate);
    }

    @Override
    public long sumEffectivePalletsByPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.sumEffectivePalletsByPeriod(startDate, endDate);
    }

    @Override
    public long countCanceledByPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.countCanceledByPeriod(startDate, endDate);
    }

    @Override
    public long sumDailyPalletsByDate(LocalDate date) {
        return nullSafe(repository.sumDailyPalletsByDate(date));
    }

    @Override
    public long sumDailyPalletsByDateExcluding(LocalDate date, Long appointmentId) {
        return nullSafe(repository.sumDailyPalletsByDateExcluding(date, appointmentId));
    }

    @Override
    public long sumSlotPalletsByDateAndTimeSlot(LocalDate date, Long timeSlotId) {
        return nullSafe(repository.sumSlotPalletsByDateAndTimeSlot(date, timeSlotId));
    }

    @Override
    public long sumSlotPalletsByDateAndTimeSlotExcluding(LocalDate date, Long timeSlotId,
                                                         Long appointmentId) {
        return nullSafe(repository.sumSlotPalletsByDateAndTimeSlotExcluding(
                date, timeSlotId, appointmentId));
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return repository.existsByOrderId(orderId);
    }

    @Override
    public boolean existsByOrderIdAndIdNot(Long orderId, Long appointmentId) {
        return repository.existsByOrderIdAndIdNot(orderId, appointmentId);
    }

    @Override
    public boolean existsByTruckTypeId(Long truckTypeId) {
        return repository.existsByTruckTypeId(truckTypeId);
    }

    @Override
    public boolean existsByTimeSlotId(Long timeSlotId) {
        return repository.existsByTimeSlotId(timeSlotId);
    }

    @Override
    public boolean existsByPalletizationTypeId(Long palletizationTypeId) {
        return repository.existsByPalletizationTypeId(palletizationTypeId);
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
