package com.shop.system.service;

import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.ProductCategoryResponse;
import com.shop.system.dto.response.ProductDetailResponse;
import com.shop.system.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ProductService {


    Page<ProductResponse> getProducts(int page, int size,
                                      String search, String category,
                                      boolean archived);

    ProductDetailResponse getProduct(UUID id);

    List<ProductCategoryResponse> getCategories();

    ProductDetailResponse createProduct(ProductCreateRequest request);

    ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request);

    void archiveProduct(UUID id);

    void unarchiveProduct(UUID id);

}
