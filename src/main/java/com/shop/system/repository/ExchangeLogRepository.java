// src/main/java/com/shop/system/repository/ExchangeLogRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.ExchangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExchangeLogRepository extends JpaRepository<ExchangeLog, UUID> {
}