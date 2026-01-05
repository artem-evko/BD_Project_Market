// src/main/java/com/shop/system/repository/PriceListItemRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, UUID> {
}