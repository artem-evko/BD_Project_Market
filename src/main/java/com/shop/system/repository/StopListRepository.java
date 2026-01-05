// src/main/java/com/shop/system/repository/StopListRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.StopList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StopListRepository extends JpaRepository<StopList, UUID> {
}