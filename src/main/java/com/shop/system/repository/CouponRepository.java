// src/main/java/com/shop/system/repository/CouponRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
}
