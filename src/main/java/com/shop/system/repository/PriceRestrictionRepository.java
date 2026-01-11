package com.shop.system.repository;

import com.shop.system.domain.entity.PriceRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PriceRestrictionRepository extends JpaRepository<PriceRestriction, UUID> {

    @Query("""
    select pr
    from PriceRestriction pr
    where pr.storageLocation is null or pr.storageLocation.id = :storageLocationId
    order by pr.priority desc
""")
    List<PriceRestriction> findForStoreWithGlobals(@Param("storageLocationId") UUID storageLocationId);

}
