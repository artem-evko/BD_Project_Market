// src/main/java/com/shop/system/repository/ShiftRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
}
