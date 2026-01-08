package com.shop.system.service;

import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductService {


    Page<ProductResponse> getProducts(int page, int size,
                                      String search, String category,
                                      Boolean archived);

    ProductDetailResponse getProduct(UUID id);

    List<ProductCategoryResponse> getCategories();

    ProductDetailResponse createProduct(ProductCreateRequest request);

    ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request);

    void archiveProduct(UUID id);

    void unarchiveProduct(UUID id);

    List<ProductLocationZoneResponse> getProductLocations(
            UUID productId,
            String zoneType,
            Boolean onlyAvailable
    );

    Page<ProductOperationResponse> getProductOperations(
            UUID productId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    );

}
