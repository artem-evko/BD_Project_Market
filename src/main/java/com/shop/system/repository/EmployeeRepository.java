package com.shop.system.repository;

import com.shop.system.domain.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    @Query("""
    select e from Employee e
    left join fetch e.position p
    left join fetch e.department d
    left join fetch e.userAccount ua
    left join fetch ua.role r
    where
        (:search is null or e.fullName ilike concat('%', :search, '%'))
    and (:departmentId is null or d.id = :departmentId)
    and (:positionId is null or p.id = :positionId)
    and (
            (:status is null and e.employmentStatus <> 'archived')
         or (:status = 'all')
         or (e.employmentStatus = :status)
        )
    """)
    Page<Employee> findEmployees(
            @Param("search") String search,
            @Param("departmentId") UUID departmentId,
            @Param("positionId") UUID positionId,
            @Param("status") String status,
            Pageable pageable
    );


    @Query("""
        select e from Employee e
        left join fetch e.position p
        left join fetch e.department d
        left join fetch e.userAccount ua
        left join fetch ua.role r
        where e.id = :id
        """)
    Optional<Employee> findWithDetailsById(@Param("id") UUID id);

    List<Employee> findByStorageLocation_IdAndEmploymentStatus(UUID storageLocationId, String employmentStatus);

    List<Employee> findByStorageLocation_Id(UUID storageLocationId);
}
