// src/main/java/com/shop/system/repository/PriceListRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, UUID> {
}