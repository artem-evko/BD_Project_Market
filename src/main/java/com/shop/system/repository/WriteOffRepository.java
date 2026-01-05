// src/main/java/com/shop/system/repository/WriteOffRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.WriteOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WriteOffRepository extends JpaRepository<WriteOff, UUID> {
}