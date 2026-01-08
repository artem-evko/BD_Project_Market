package com.shop.system.repository;

import com.shop.system.domain.entity.BatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BatchLocationRepository extends JpaRepository<BatchLocation, UUID> {

    @Query("""
    select bl
    from BatchLocation bl
         join fetch bl.batch b
         join fetch b.product p
         join fetch bl.storageZone sz
    where p.id = :productId
      and sz.storageLocation.id = :storageLocationId
      and sz.isActive = true
      and (:zoneType is null or sz.zoneType = :zoneType)
      and (:onlyAvailable = false or bl.quantity > 0)
""")
    List<BatchLocation> findProductLocations(
            @Param("productId") UUID productId,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("zoneType") String zoneType,
            @Param("onlyAvailable") boolean onlyAvailable
    );
}
