// src/main/java/com/shop/system/repository/VacationRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, UUID> {
}