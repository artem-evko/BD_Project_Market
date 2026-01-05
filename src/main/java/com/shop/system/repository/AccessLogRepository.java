// src/main/java/com/shop/system/repository/AccessLogRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {
}