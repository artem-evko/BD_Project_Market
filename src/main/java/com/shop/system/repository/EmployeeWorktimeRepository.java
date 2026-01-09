package com.shop.system.repository;

import com.shop.system.domain.entity.EmployeeWorktime;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeWorktimeRepository extends JpaRepository<EmployeeWorktime, UUID> {

    @Query("""
        select ew from EmployeeWorktime ew
        join fetch ew.employee e
        where (:employeeId is null or e.id = :employeeId)
          and ew.date >= coalesce(:dateFrom, ew.date)
          and ew.date <= coalesce(:dateTo, ew.date)
        order by ew.date desc, ew.scheduledStart desc
    """)
    Page<EmployeeWorktime> findForList(
            @Param("employeeId") UUID employeeId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    @Query("""
        select wt from EmployeeWorktime wt
        join fetch wt.employee e
        where e.id = :employeeId
          and wt.actualLogout is null
        order by wt.date desc, wt.scheduledStart desc
    """)
    Optional<EmployeeWorktime> findOpenByEmployee(@Param("employeeId") UUID employeeId);

    @Query("""
        select wt from EmployeeWorktime wt
        left join fetch wt.employee e
        where wt.id = :id
    """)
    Optional<EmployeeWorktime> findWithEmployee(@Param("id") UUID id);

    @Query("""
      select ew from EmployeeWorktime ew
      join fetch ew.employee e
      where e.storageLocation.id = :storageLocationId
        and ew.date >= :dateFrom
        and ew.date <= :dateTo
    """)
    List<EmployeeWorktime> findForReport(
            @Param("storageLocationId") UUID storageLocationId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}
