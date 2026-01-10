package com.shop.system.repository;

import com.shop.system.domain.entity.EmployeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, UUID> {

    // “моё расписание”
    List<EmployeeSchedule> findByEmployee_IdAndDateBetweenOrderByDateAsc(UUID employeeId, LocalDate from, LocalDate to);

    // “все сотрудники ТТ”
    List<EmployeeSchedule> findByEmployee_StorageLocation_IdAndDateBetweenOrderByEmployee_FullNameAscDateAsc(
            UUID storageLocationId, LocalDate from, LocalDate to
    );

    // нужно для update
    Optional<EmployeeSchedule> findById(UUID id);

    // удобно для генерации (чтобы не делать N запросов на каждый день)
    List<EmployeeSchedule> findByEmployee_StorageLocation_IdAndDateBetween(UUID storageLocationId, LocalDate from, LocalDate to);

    Optional<EmployeeSchedule> findByEmployee_IdAndDate(UUID employeeId, LocalDate date);
}
