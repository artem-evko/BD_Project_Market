package com.shop.system.repository;

import com.shop.system.domain.entity.StorageZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StorageZoneRepository extends JpaRepository<StorageZone, UUID> {

    List<StorageZone> findByStorageLocationIdAndIsActiveTrueOrderByNameAsc(UUID storageLocationId);
}
