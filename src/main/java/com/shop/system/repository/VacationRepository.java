// src/main/java/com/shop/system/repository/VacationRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Vacation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VacationRepository extends JpaRepository<Vacation, UUID> {

    @Query("""
    select v from Vacation v
    join fetch v.employee e
    where (:employeeId is null or e.id = :employeeId)
      and (:status is null or :status = 'all' or v.status = :status)
      and (:dateFrom is null or v.dateStart >= :dateFrom)
      and (:dateTo   is null or v.dateEnd   <= :dateTo)
    order by v.createdAt desc
  """)
    Page<Vacation> findForList(
            @Param("employeeId") UUID employeeId,
            @Param("status") String status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    @Query("""
    select v from Vacation v
    left join fetch v.employee e
    left join fetch v.approvedByEmployee abe
    where v.id = :id
  """)
    Optional<Vacation> findWithDetails(@Param("id") UUID id);

    // пересечения (для валидации)
    @Query("""
    select count(v) from Vacation v
    where v.employee.id = :employeeId
      and v.status <> 'rejected'
      and v.dateStart <= :endDate
      and v.dateEnd >= :startDate
      and (:excludeId is null or v.id <> :excludeId)
  """)
    long countOverlaps(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId
    );
}