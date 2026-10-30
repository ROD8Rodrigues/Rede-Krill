package com.redekrill.scheduling.infrastructure.persistence.adapter;

import com.redekrill.scheduling.domain.model.Appointment;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.AppointmentRepository;
import com.redekrill.scheduling.infrastructure.persistence.entity.AppointmentJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.TimeSlotJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.TruckTypeJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.UserJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.entity.PalletizationTypeJpaEntity;
import com.redekrill.scheduling.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.redekrill.scheduling.infrastructure.persistence.repository.AppointmentJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
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
        entity.setCreatedBy(entityManager.getReference(UserJpaEntity.class, appointment.createdBy().id()));
        entity.setCreatedAt(appointment.createdAt());
        entity.setUpdatedAt(appointment.updatedAt());

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
    public List<Appointment> findByPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.findByPeriod(startDate, endDate).stream()
                .map(AppointmentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long sumEffectivePalletsByDate(LocalDate date) {
        return nullSafe(repository.sumEffectivePalletsByDate(date));
    }

    @Override
    public long sumEffectivePalletsByDateExcluding(LocalDate date, Long appointmentId) {
        return nullSafe(repository.sumEffectivePalletsByDateExcluding(date, appointmentId));
    }

    @Override
    public long sumEffectivePalletsByDateAndTimeSlot(LocalDate date, Long timeSlotId) {
        return nullSafe(repository.sumEffectivePalletsByDateAndTimeSlot(date, timeSlotId));
    }

    @Override
    public long sumEffectivePalletsByDateAndTimeSlotExcluding(LocalDate date, Long timeSlotId,
                                                              Long appointmentId) {
        return nullSafe(repository.sumEffectivePalletsByDateAndTimeSlotExcluding(
                date, timeSlotId, appointmentId));
    }

    @Override
    public boolean existsByTruckTypeId(Long truckTypeId) {
        return repository.existsByTruckTypeId(truckTypeId);
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
