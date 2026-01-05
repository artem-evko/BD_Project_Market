// src/main/java/com/shop/system/repository/EmployeeRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
}