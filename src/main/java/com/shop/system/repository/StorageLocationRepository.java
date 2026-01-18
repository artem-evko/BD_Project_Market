package com.shop.system.repository;

import com.shop.system.domain.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {

    @Query(value = "select id, name from storage_locations order by name", nativeQuery = true)
    List<Object[]> findIdAndNameOrdered();
}
