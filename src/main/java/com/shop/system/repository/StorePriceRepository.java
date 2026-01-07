package com.shop.system.repository;

import com.shop.system.domain.entity.Product;
import com.shop.system.domain.entity.StorageLocation;
import com.shop.system.domain.entity.StorePrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StorePriceRepository extends JpaRepository<StorePrice, UUID> {

    Optional<StorePrice> findFirstByStorageLocationAndProductAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            StorageLocation storageLocation,
            Product product,
            LocalDate effectiveDate
    );
}
