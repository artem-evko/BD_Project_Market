// src/main/java/com/shop/system/repository/PositionRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
}