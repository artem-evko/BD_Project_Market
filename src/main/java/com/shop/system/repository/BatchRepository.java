// src/main/java/com/shop/system/repository/BatchRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {
}
