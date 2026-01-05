// src/main/java/com/shop/system/repository/TruckRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TruckRepository extends JpaRepository<Truck, UUID> {
}