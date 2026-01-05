// src/main/java/com/shop/system/repository/ManufacturerRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {
}