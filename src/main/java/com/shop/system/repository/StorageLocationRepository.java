// src/main/java/com/shop/system/repository/StorageLocationRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {
}