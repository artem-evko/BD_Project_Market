// src/main/java/com/shop/system/repository/PriceRestrictionRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.PriceRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriceRestrictionRepository extends JpaRepository<PriceRestriction, UUID> {
}
