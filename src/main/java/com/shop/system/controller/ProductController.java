package com.shop.system.controller;

import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.*;
import com.shop.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean includeArchived
    ) {
        return productService.getProducts(page, size, search, category, includeArchived);

    }

    @GetMapping("/{id}")
    public ProductDetailResponse getProductById(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @GetMapping("/categories")
    public List<ProductCategoryResponse> getProductCategories() {
        return productService.getCategories();
    }

    @PostMapping
    public ProductDetailResponse createProduct(@RequestBody ProductCreateRequest request)
    {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductDetailResponse updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductUpdateRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        productService.archiveProduct(id);
    }

    @PatchMapping("/{id}/unarchive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unarchive(@PathVariable UUID id) {
        productService.unarchiveProduct(id);
    }

    @GetMapping("/{id}/locations")
    public List<ProductLocationZoneResponse> getProductLocations(
            @PathVariable UUID id,
            @RequestParam(required = false) String zoneType,
            @RequestParam(required = false, defaultValue = "true") boolean onlyAvailable
    ) {
        return productService.getProductLocations(id, zoneType, onlyAvailable);
    }

    @GetMapping("/{id}/operations")
    public Page<ProductOperationResponse> getProductOperations(
            @PathVariable UUID id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.getProductOperations(id, type, dateFrom, dateTo, page, size);
    }



}
