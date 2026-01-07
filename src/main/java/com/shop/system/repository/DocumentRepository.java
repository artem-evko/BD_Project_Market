// src/main/java/com/shop/system/repository/DocumentRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
