// src/main/java/com/shop/system/repository/InventoryRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    @Query("""
        select i from Inventory i
        where i.storageLocation.id = :slId
          and (:status is null or :status = 'all' or i.status = :status)
          and (:dateFrom is null or i.inventoryDate >= :dateFrom)
          and (:dateTo is null or i.inventoryDate <= :dateTo)
        order by i.inventoryDate desc
    """)
    Page<Inventory> findList(
            @Param("slId") UUID storageLocationId,
            @Param("status") String status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    @Query("""
        select i from Inventory i
        left join fetch i.employee e
        left join fetch i.storageLocation sl
        where i.id = :id
    """)
    Optional<Inventory> findWithDetails(@Param("id") UUID id);
}