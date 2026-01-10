package com.shop.system.repository;

import com.shop.system.domain.entity.SupplyInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplyInvoiceItemRepository extends JpaRepository<SupplyInvoiceItem, UUID> {

    List<SupplyInvoiceItem> findBySupplyInvoiceIdOrderByLineNo(UUID supplyInvoiceId);

    Optional<SupplyInvoiceItem> findBySupplyInvoiceIdAndLineNo(UUID supplyInvoiceId, Integer lineNo);

    Optional<SupplyInvoiceItem> findByDiscrepancyRequestId(UUID discrepancyRequestId);
}
