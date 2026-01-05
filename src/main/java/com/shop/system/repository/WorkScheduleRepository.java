// src/main/java/com/shop/system/repository/WorkScheduleRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.WorkScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkScheduleTemplate, UUID> {
}