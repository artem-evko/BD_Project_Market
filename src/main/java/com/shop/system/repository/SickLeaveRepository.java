// src/main/java/com/shop/system/repository/SickLeaveRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.SickLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SickLeaveRepository extends JpaRepository<SickLeave, UUID> {
}