// src/main/java/com/shop/system/repository/StorageZoneRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.StorageZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StorageZoneRepository extends JpaRepository<StorageZone, UUID> {
}