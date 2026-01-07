package com.shop.system.repository;

import com.shop.system.domain.entity.Product;
import com.shop.system.domain.entity.ProductCategory;
import com.shop.system.domain.entity.ProductCategoryLink;
import org.springframework.data.jpa.repository.*;

import java.util.*;

public interface ProductCategoryLinkRepository extends JpaRepository<ProductCategoryLink, UUID> {

    long countByCategory(ProductCategory category);

    List<ProductCategoryLink> findAllByProduct(Product product);

    void deleteByProduct(Product product);

    boolean existsByProductAndCategory(Product product, ProductCategory category);
}
