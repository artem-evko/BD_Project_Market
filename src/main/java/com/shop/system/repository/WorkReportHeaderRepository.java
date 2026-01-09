package com.shop.system.repository;

import com.shop.system.domain.entity.WorkReportHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public interface WorkReportHeaderRepository extends JpaRepository<WorkReportHeader, UUID> {

    @Query("""
select h from WorkReportHeader h
where h.storageLocation.id = :storageLocationId
  and (coalesce(:weekStart, h.weekStart) = h.weekStart)
  and (:status is null or :status = 'all' or h.status = :status)
order by h.weekStart desc, h.createdAt desc
""")
    Page<WorkReportHeader> findForList(
            @Param("storageLocationId") UUID storageLocationId,
            @Param("weekStart") LocalDate weekStart,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
      select h from WorkReportHeader h
      left join fetch h.storageLocation sl
      left join fetch h.director d
      where h.id = :id
    """)
    Optional<WorkReportHeader> findWithDetails(@Param("id") UUID id);

    Optional<WorkReportHeader> findByStorageLocation_IdAndWeekStart(UUID storageLocationId, LocalDate weekStart);
}
