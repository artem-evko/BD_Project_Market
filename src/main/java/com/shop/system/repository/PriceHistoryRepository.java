// src/main/java/com/shop/system/repository/PriceHistoryRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
    List<PriceHistory> findByProductIdAndChangeDateAfterOrderByChangeDateDesc(UUID productId, Instant from);
}