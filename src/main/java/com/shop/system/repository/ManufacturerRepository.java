// src/main/java/com/shop/system/repository/ManufacturerRepository.java
package com.shop.system.repository;

import com.shop.system.domain.entity.Manufacturer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {

    List<Manufacturer> findByManufacturerNameIgnoreCaseContaining(String namePart);

    @Query("""
        select m from Manufacturer m
        where (:search is null or :search = ''
               or lower(m.manufacturerName) like lower(concat('%', :search, '%'))
               or lower(m.manufacturerCountry) like lower(concat('%', :search, '%')))
        order by m.manufacturerName
    """)
    List<Manufacturer> search(String search, Pageable pageable);
}