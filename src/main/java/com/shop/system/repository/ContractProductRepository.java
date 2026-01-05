// src/main/java/com/shop/system/repository/ContractProductRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.ContractProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractProductRepository extends JpaRepository<ContractProduct, UUID> {
}
