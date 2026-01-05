// src/main/java/com/shop/system/repository/ProductCategoryRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
}