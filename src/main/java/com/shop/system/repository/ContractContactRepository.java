// src/main/java/com/shop/system/repository/ContractContactRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.ContractContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractContactRepository extends JpaRepository<ContractContact, UUID> {
}