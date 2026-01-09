// src/main/java/com/shop/system/repository/PriceListItemRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, UUID> {
    @Query("""
        select pli from PriceListItem pli
        join fetch pli.product p
        where pli.storageLocation.id = :storageLocationId
        order by p.name asc
    """)
    List<PriceListItem> findByStorageLocation_Id(@Param("storageLocationId") UUID storageLocationId);
    Optional<PriceListItem> findTopByProduct_IdOrderByPriceList_EffectiveDateDesc(UUID productId);


}