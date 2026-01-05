// src/main/java/com/shop/system/repository/BatchLocationRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.BatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BatchLocationRepository extends JpaRepository<BatchLocation, UUID> {
}