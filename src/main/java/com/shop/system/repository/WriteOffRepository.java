package com.shop.system.repository;

import com.shop.system.domain.entity.WriteOff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WriteOffRepository extends JpaRepository<WriteOff, UUID> {

    @Query("""
        select w from WriteOff w
        join w.storageZone sz
        join sz.storageLocation sl
        where sl.id = :storageLocationId
          and ( :status is null or w.status = :status)
        """)
    Page<WriteOff> findByLocationAndStatus(
            @Param("storageLocationId") UUID storageLocationId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
        select w from WriteOff w
        join w.storageZone sz
        join sz.storageLocation sl
        where w.id = :id and sl.id = :storageLocationId
        """)
    Optional<WriteOff> findByIdAndLocation(
            @Param("id") UUID id,
            @Param("storageLocationId") UUID storageLocationId
    );
}
