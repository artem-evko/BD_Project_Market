// src/main/java/com/shop/system/repository/DepartmentRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
}   