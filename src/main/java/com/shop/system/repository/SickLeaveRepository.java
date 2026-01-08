package com.shop.system.repository;

import com.shop.system.domain.entity.SickLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SickLeaveRepository extends JpaRepository<SickLeave, UUID> {

    /**
     * Листинг с фильтрами.
     * docStatus: pending|received|overdue (по ТЗ). overdue считаем на лету.
     */
    @EntityGraph(attributePaths = {"employee", "approvedByEmployee"})
    @Query("""
        select sl from SickLeave sl
        where (:employeeId is null or sl.employee.id = :employeeId)
          and (:status is null or sl.status = :status)
          and (:dateFrom is null or sl.dateStart >= :dateFrom)
          and (:dateTo is null or sl.dateEnd <= :dateTo)
          and (
                :docStatus is null
                or (
                    :docStatus = 'pending'
                    and (sl.docReceivedAt is null)
                )
                or (
                    :docStatus = 'received'
                    and (sl.docReceivedAt is not null)
                )
                or (
                    :docStatus = 'overdue'
                    and (sl.docReceivedAt is null)
                    and sl.docDueDate is not null
                    and sl.docDueDate < CURRENT_DATE
                )
          )
        """)
    Page<SickLeave> findForList(
            @Param("employeeId") UUID employeeId,
            @Param("status") String status,
            @Param("docStatus") String docStatus,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    /**
     * Проверка пересечений по датам:
     * пересечение если существующий.start <= end AND существующий.end >= start
     */
    @Query("""
        select count(sl) from SickLeave sl
        where sl.employee.id = :employeeId
          and (:excludeId is null or sl.id <> :excludeId)
          and sl.dateStart <= :end
          and sl.dateEnd >= :start
        """)
    long countOverlaps(
            @Param("employeeId") UUID employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("excludeId") UUID excludeId
    );

    @EntityGraph(attributePaths = {"employee", "approvedByEmployee"})
    @Query("select sl from SickLeave sl where sl.id = :id")
    Optional<SickLeave> findWithDetails(@Param("id") UUID id);
}
