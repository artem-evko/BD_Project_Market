package com.shop.system.repository;

import com.shop.system.domain.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    @Query("""
        select c.id, c.name, count(distinct p.id)
        from ProductCategory c
        left join com.shop.system.domain.entity.ProductCategoryLink pcl
            on pcl.category = c
        left join pcl.product p
            on p.archived = false
        group by c.id, c.name
        order by c.name
        """)
    List<Object[]> findAllWithProductCount();
}
