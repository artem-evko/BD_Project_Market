// src/main/java/com/shop/system/repository/SupplyInvoiceRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.SupplyInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplyInvoiceRepository extends JpaRepository<SupplyInvoice, UUID> {
}