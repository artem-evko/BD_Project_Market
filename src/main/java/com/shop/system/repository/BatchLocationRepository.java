package com.shop.system.repository;

import com.shop.system.domain.entity.BatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

    @Query("""
        select bl
        from BatchLocation bl
        join bl.batch b
        join bl.storageZone z
        where b.product.id = :productId
          and z.storageLocation.id = :storageLocationId
          and z.isActive = true
          and (:onlyAvailable = false or bl.quantity > 0)
        """)
    List<BatchLocation> findByProductAndLocation(
            @Param("productId") UUID productId,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("onlyAvailable") boolean onlyAvailable
    );

    @Query("""
        select coalesce(sum(bl.quantity), 0)
        from BatchLocation bl
        where bl.batch.id = :batchId
          and bl.storageZone.id = :storageZoneId
        """)
    BigDecimal getQuantityForBatchInZone(
            @Param("batchId") UUID batchId,
            @Param("storageZoneId") UUID storageZoneId
    );

    @Query("""
        select coalesce(sum(bl.quantity), 0)
        from BatchLocation bl
        join bl.storageZone z
        where bl.batch.id = :batchId
          and z.storageLocation.id = :storageLocationId
          and z.isActive = true
    """)
    BigDecimal sumQuantityForBatchInLocation(
            @Param("batchId") UUID batchId,
            @Param("storageLocationId") UUID storageLocationId
    );

    Optional<BatchLocation> findByBatchIdAndStorageZoneId(UUID batchId, UUID storageZoneId);

    List<BatchLocation> findTop1ByBatch_Product_IdAndStorageZone_IdAndStorageZone_StorageLocation_IdAndQuantityGreaterThanOrderByBatch_ExpirationDateAsc(
            UUID productId,
            UUID storageZoneId,
            UUID storageLocationId,
            BigDecimal minQuantity
    );

}
