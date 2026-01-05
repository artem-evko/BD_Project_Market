// src/main/java/com/shop/system/repository/WorkReportRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.WorkReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkReportRepository extends JpaRepository<WorkReport, UUID> {
}
