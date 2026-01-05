// src/main/java/com/shop/system/repository/WarehouseOperationRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.WarehouseOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WarehouseOperationRepository extends JpaRepository<WarehouseOperation, UUID> {
}