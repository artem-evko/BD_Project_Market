package com.shop.system.repository;

import com.shop.system.domain.entity.WorkScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkScheduleTemplateRepository extends JpaRepository<WorkScheduleTemplate, UUID> {

    List<WorkScheduleTemplate> findByEmployee_StorageLocation_Id(UUID storageLocationId);

    List<WorkScheduleTemplate> findByEmployee_Id(UUID employeeId);
}
