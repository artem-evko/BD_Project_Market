// src/main/java/com/shop/system/repository/InventoryItemRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
}