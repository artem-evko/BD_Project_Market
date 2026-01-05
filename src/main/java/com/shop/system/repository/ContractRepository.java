// src/main/java/com/shop/system/repository/ContractRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
}