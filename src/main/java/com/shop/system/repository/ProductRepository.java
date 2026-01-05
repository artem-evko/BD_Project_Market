// src/main/java/com/shop/system/repository/ProductRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
