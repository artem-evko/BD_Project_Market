package com.shop.system.repository;

import com.shop.system.domain.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @Query("""
        select ii
        from InventoryItem ii
        join fetch ii.product p
        left join fetch ii.batch b
        where ii.inventory.id = :inventoryId
    """)
    List<InventoryItem> findByInventoryIdWithRefs(@Param("inventoryId") UUID inventoryId);

    Optional<InventoryItem> findByIdAndInventory_Id(UUID id, UUID inventoryId);

    long countByInventory_Id(UUID inventoryId);
}
