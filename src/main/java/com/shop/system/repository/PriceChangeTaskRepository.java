// src/main/java/com/shop/system/repository/PriceChangeTaskRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceChangeTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceChangeTaskRepository extends JpaRepository<PriceChangeTask, UUID> {
}
