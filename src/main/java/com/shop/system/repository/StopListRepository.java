// src/main/java/com/shop/system/repository/StopListRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.StopList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StopListRepository extends JpaRepository<StopList, UUID> {

    List<StopList> findAllByOrderByCreatedAtDesc();
    boolean existsByStorageLocation_IdAndProduct_IdAndEffectiveDate(UUID storageLocationId, UUID productId, LocalDate effectiveDate);

}