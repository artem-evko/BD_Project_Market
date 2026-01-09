package com.shop.system.repository;

import com.shop.system.domain.entity.WarehouseOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WarehouseOperationRepository extends JpaRepository<WarehouseOperation, UUID> {

    @Query("""
        select wo
        from WarehouseOperation wo
        left join fetch wo.fromZone fz
        left join fetch wo.toZone tz
        join fetch wo.employee e
        where wo.product.id = :productId
          and (
                (wo.fromZone is not null and fz.storageLocation.id = :storageLocationId)
             or (wo.toZone is not null and tz.storageLocation.id = :storageLocationId)
          )
          and (coalesce(:type, wo.type) = wo.type)
          and (coalesce(:dateFrom, wo.operationDate) <= wo.operationDate)
          and (coalesce(:dateTo, wo.operationDate) > wo.operationDate)
        order by wo.operationDate desc
        """)
    Page<WarehouseOperation> findProductOperations(
            @Param("productId") UUID productId,
            @Param("storageLocationId") UUID storageLocationId,
            @Param("type") String type,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable
    );

    @Query("""
        select wo
        from WarehouseOperation wo
        left join fetch wo.fromZone fz
        left join fetch wo.toZone tz
        left join fetch wo.employee e
        left join fetch wo.product p
        where (fz.storageLocation.id = :storageLocationId or tz.storageLocation.id = :storageLocationId)
          and wo.operationDate >= :fromInstant
          and wo.operationDate < :toInstant
        order by wo.operationDate desc
        """)
    List<WarehouseOperation> findForLocationInPeriod(
            @Param("storageLocationId") UUID storageLocationId,
            @Param("fromInstant") Instant fromInstant,
            @Param("toInstant") Instant toInstant
    );
}
