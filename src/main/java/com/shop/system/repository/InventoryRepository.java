// src/main/java/com/shop/system/repository/InventoryRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
}