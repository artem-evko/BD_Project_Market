package com.shop.system.repository;

import com.shop.system.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.categories c
    WHERE p.archived = false
      AND (
        :query IS NULL
        OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (
        :category IS NULL
        OR LOWER(c.name) = LOWER(:category)
      )
""")
    Page<Product> searchProducts(
            @Param("query") String query,
            @Param("category") String category,
            Pageable pageable
    );

    @Query("""
        select distinct p
        from Product p
        left join p.categoryLinks pcl
        left join pcl.category c
         where (
              :search is null
              or lower(p.name) like lower(concat('%', :search, '%'))
              or lower(p.barcode) like lower(concat('%', :search, '%'))
          )
          and (
              :category is null
              or lower(c.name) = lower(:category)
          )
        """)
    Page<Product> searchProductsIncludingArchived(@Param("search") String search,
                                 @Param("category") String category,
                                 Pageable pageable);
}
