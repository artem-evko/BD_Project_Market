package com.shop.system.repository;

import com.shop.system.domain.entity.StorageZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageZoneRepository extends JpaRepository<StorageZone, UUID> {

    List<StorageZone> findByStorageLocationIdAndIsActiveTrueOrderByNameAsc(UUID storageLocationId);

    @Query("""
        select z
        from StorageZone z
        where z.storageLocation.id = :storageLocationId
          and z.isActive = true
        order by z.zoneType asc, z.name asc
    """)
    Optional<StorageZone> findFirstActiveForLocation(@Param("storageLocationId") UUID storageLocationId);
}
