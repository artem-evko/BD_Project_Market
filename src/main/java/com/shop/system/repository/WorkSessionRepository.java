// src/main/java/com/shop/system/repository/WorkSessionRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.WorkSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkSessionRepository extends JpaRepository<WorkSession, UUID> {
}