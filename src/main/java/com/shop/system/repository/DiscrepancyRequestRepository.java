// src/main/java/com/shop/system/repository/DiscrepancyRequestRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.DiscrepancyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DiscrepancyRequestRepository extends JpaRepository<DiscrepancyRequest, UUID> {
}