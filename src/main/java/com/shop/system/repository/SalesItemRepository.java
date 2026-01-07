// src/main/java/com/shop/system/repository/SalesItemRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.SalesItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SalesItemRepository extends JpaRepository<SalesItem, UUID> {
}
