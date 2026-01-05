// src/main/java/com/shop/system/repository/ContractorRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, UUID> {
}