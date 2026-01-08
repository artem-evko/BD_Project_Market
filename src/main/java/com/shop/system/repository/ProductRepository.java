package com.shop.system.repository;

import com.shop.system.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
    select distinct p
    from Product p
    join StorePrice sp
        on sp.product = p
       and sp.storageLocation.id = :storageLocationId
       and sp.effectiveDate <= :today
    left join p.categoryLinks cl
    left join cl.category c
    where
        (:includeArchived = true or p.archived = false)
        and (:pattern is null
             or lower(p.name) like :pattern
             or lower(p.barcode) like :pattern)
        and (:categoryPattern is null
             or lower(c.name) like :categoryPattern)
""")
    Page<Product> searchProductsForLocation(
            @Param("storageLocationId") UUID storageLocationId,
            @Param("pattern") String pattern,
            @Param("categoryPattern") String categoryPattern,
            @Param("includeArchived") boolean includeArchived,
            @Param("today") LocalDate today,
            Pageable pageable
    );

}
