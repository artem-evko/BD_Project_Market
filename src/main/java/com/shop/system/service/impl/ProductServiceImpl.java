package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.*;
import com.shop.system.exception.EntityNotFoundException;
import com.shop.system.mapper.ProductMapper;
import com.shop.system.repository.*;
import com.shop.system.service.ProductService;
import com.shop.system.service.context.StorageLocationContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StorePriceRepository storePriceRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryLinkRepository productCategoryLinkRepository;
    private final ProductMapper productMapper;
    private final ManufacturerRepository manufacturerRepository;
    private final BatchLocationRepository batchLocationRepository;
    private final WarehouseOperationRepository warehouseOperationRepository;
    private final StorageLocationContextService storageLocationContextService;

    @Override
    public Page<ProductResponse> getProducts(
            int page, int size,
            String search, String category,
            Boolean includeArchived
    ) {
        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("name").ascending()
        );

        String normalizedSearch   = normalize(search);
        String normalizedCategory = normalize(category);

        String pattern = null;
        if (normalizedSearch != null) {
            pattern = "%" + normalizedSearch.toLowerCase() + "%";
        }

        String categoryPattern = null;
        if (normalizedCategory != null) {
            categoryPattern = "%" + normalizedCategory.toLowerCase() + "%";
        }

        boolean includeArchivedEffective = Boolean.TRUE.equals(includeArchived);

        log.info(
                "GET PRODUCTS: search='{}', category='{}', pattern='{}', catPattern='{}', includeArchived={}",
                normalizedSearch, normalizedCategory, pattern, categoryPattern, includeArchivedEffective
        );

        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Page<Product> productPage = productRepository.searchProductsForLocation(
                storageLocation.getId(),
                pattern,
                categoryPattern,
                includeArchivedEffective,
                today,
                pageable
        );

        List<ProductResponse> mapped = productPage
                .getContent()
                .stream()
                .map(product -> {
                    BigDecimal currentPrice = resolveCurrentPrice(storageLocation, product, today);
                    ProductResponse dto = productMapper.toListItem(product, currentPrice);
                    BigDecimal totalQty = calculateTotalQuantityForProductLocation(product.getId(), storageLocation.getId());
                    dto.setTotalQuantity(totalQty);
                    return dto;
                })
                .toList();

        return new PageImpl<>(
                mapped,
                pageable,
                productPage.getTotalElements()
        );
    }

    private BigDecimal calculateTotalQuantityForProductLocation(UUID productId, UUID storageLocationId) {
        List<BatchLocation> locations = batchLocationRepository.findByProductAndLocation(
                productId, storageLocationId, true);
        return locations.stream()
                .map(bl -> bl.getQuantity() == null ? BigDecimal.ZERO : bl.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    @Override
    public ProductDetailResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product id = " + id));

        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        // суммарный остаток по партиям в текущей ТТ
        BigDecimal totalQty = calculateTotalQuantityForProductLocation(
                product.getId(),
                storageLocation.getId()
        );

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        ProductDetailResponse dto = productMapper.toDetail(product);

        dto.setTotalQuantity(totalQty);

        return dto;
    }

    private BigDecimal resolveCurrentPrice(StorageLocation storageLocation, Product product, LocalDate date) {
        return storePriceRepository
                .findFirstByStorageLocationIdAndProductIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        storageLocation.getId(),
                        product.getId(),
                        date
                )
                .map(StorePrice::getPrice)
                .orElse(null);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public List<ProductCategoryResponse> getCategories() {
        List<ProductCategory> categories = productCategoryRepository.findAll(Sort.by("name").ascending());

        return categories.stream()
                .map(cat -> {
                    long count = productCategoryLinkRepository.countByCategory(cat);
                    return ProductCategoryResponse.builder()
                            .id(cat.getId())
                            .name(cat.getName())
                            .productCount((int) count)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new EntityNotFoundException("Manufacturer not found"));

        Product product = productMapper.fromCreateRequest(request, manufacturer);
        Product savedProduct = productRepository.save(product);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<ProductCategory> categories = productCategoryRepository.findAllById(request.getCategoryIds());

            List<ProductCategoryLink> links = categories.stream()
                    .map(cat -> ProductCategoryLink.builder()
                            .product(savedProduct)
                            .category(cat)
                            .build())
                    .toList();

            productCategoryLinkRepository.saveAll(links);
            savedProduct.setCategoryLinks(links);
        }

        return productMapper.toDetail(savedProduct);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        Manufacturer manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new EntityNotFoundException("Manufacturer not found"));

        productMapper.updateEntity(product, request, manufacturer);

        Set<UUID> requestedCategoryIds = request.getCategoryIds() == null
                ? Collections.emptySet()
                : new HashSet<>(request.getCategoryIds());

        List<ProductCategoryLink> existingLinks = productCategoryLinkRepository.findByProduct(product);

        Set<UUID> existingCategoryIds = existingLinks.stream()
                .map(link -> link.getCategory().getId())
                .collect(Collectors.toSet());

        Set<UUID> toRemove = new HashSet<>(existingCategoryIds);
        toRemove.removeAll(requestedCategoryIds);

        Set<UUID> toAdd = new HashSet<>(requestedCategoryIds);
        toAdd.removeAll(existingCategoryIds);

        if (!toRemove.isEmpty()) {
            List<ProductCategoryLink> linksToRemove = existingLinks.stream()
                    .filter(link -> toRemove.contains(link.getCategory().getId()))
                    .toList();

            productCategoryLinkRepository.deleteAll(linksToRemove);
        }

        if (!toAdd.isEmpty()) {
            List<ProductCategory> categoriesToAdd = productCategoryRepository.findAllById(toAdd);

            Map<UUID, ProductCategory> categoriesById = categoriesToAdd.stream()
                    .collect(Collectors.toMap(ProductCategory::getId, c -> c));

            List<ProductCategoryLink> linksToAdd = toAdd.stream()
                    .map(catId -> {
                        ProductCategory category = categoriesById.get(catId);
                        if (category == null) {
                            throw new EntityNotFoundException("Category not found: " + catId);
                        }
                        return ProductCategoryLink.builder()
                                .product(product)
                                .category(category)
                                .build();
                    })
                    .toList();

            productCategoryLinkRepository.saveAll(linksToAdd);
        }

        Product saved = productRepository.save(product);
        return productMapper.toDetail(saved);
    }

    @Override
    @Transactional
    public void archiveProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        if (Boolean.TRUE.equals(product.getArchived())) {
            return;
        }
        product.setArchived(true);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void unarchiveProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        if (Boolean.FALSE.equals(product.getArchived())) {
            return;
        }
        product.setArchived(false);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductLocationZoneResponse> getProductLocations(
            UUID productId,
            String zoneType,
            Boolean onlyAvailable
    ) {
        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        boolean onlyAvailableEffective = (onlyAvailable == null) || Boolean.TRUE.equals(onlyAvailable);

        log.info("GET PRODUCT LOCATIONS: productId={}, storageLocationId={}, zoneType='{}', onlyAvailable={}",
                productId, storageLocation.getId(), zoneType, onlyAvailableEffective);

        List<BatchLocation> locations = batchLocationRepository.findProductLocations(
                productId,
                storageLocation.getId(),
                zoneType,
                onlyAvailableEffective
        );

        Map<StorageZone, List<BatchLocation>> byZone = locations.stream()
                .collect(Collectors.groupingBy(
                        BatchLocation::getStorageZone,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return byZone.entrySet()
                .stream()
                .map(entry -> {
                    StorageZone zone = entry.getKey();
                    List<BatchLocation> zoneLocations = entry.getValue();

                    BigDecimal zoneTotalQty = zoneLocations.stream()
                            .map(bl -> bl.getQuantity() == null ? BigDecimal.ZERO : bl.getQuantity())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<ProductLocationBatchResponse> batches = zoneLocations.stream()
                            .map(bl -> {
                                Batch batch = bl.getBatch();

                                String invoiceNumber = null;
                                if (batch.getSupplyInvoice() != null) {
                                    invoiceNumber = batch.getSupplyInvoice().getInvoiceNumber();
                                }

                                return ProductLocationBatchResponse.builder()
                                        .batchId(batch.getId())
                                        .expirationDate(batch.getExpirationDate())
                                        .invoiceNumber(invoiceNumber)
                                        .quantity(bl.getQuantity())
                                        .build();
                            })
                            .toList();

                    return ProductLocationZoneResponse.builder()
                            .zoneId(zone.getId())
                            .storageZoneName(zone.getName())
                            .zoneType(zone.getZoneType())
                            .temperatureMode(zone.getTemperatureMode())
                            .zoneTotalQty(zoneTotalQty)
                            .batches(batches)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductOperationResponse> getProductOperations(
            UUID productId,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "operationDate")
        );

        ZoneId zoneId = ZoneId.systemDefault();

        Instant fromInstant = null;
        Instant toInstant = null;

        if (dateFrom != null) {
            fromInstant = dateFrom.atStartOfDay(zoneId).toInstant();
        }
        if (dateTo != null) {
            toInstant = dateTo.plusDays(1).atStartOfDay(zoneId).toInstant();
        }

        String normalizedType = type != null ? type.toUpperCase(Locale.ROOT) : null;

        log.info("GET PRODUCT OPERATIONS: productId={}, storageLocationId={}, type='{}', dateFrom={}, dateTo={}, page={}, size={}",
                productId, storageLocation.getId(), normalizedType, fromInstant, toInstant, page, size);

        Page<WarehouseOperation> operationsPage = warehouseOperationRepository.findProductOperations(
                productId,
                storageLocation.getId(),
                normalizedType,
                fromInstant,
                toInstant,
                pageable
        );

        List<ProductOperationResponse> content = operationsPage.getContent()
                .stream()
                .map(op -> ProductOperationResponse.builder()
                        .id(op.getId())
                        .operationDate(op.getOperationDate())
                        .type(op.getType())
                        .quantity(op.getQuantity())
                        .fromZone(op.getFromZone() != null ? op.getFromZone().getName() : null)
                        .toZone(op.getToZone() != null ? op.getToZone().getName() : null)
                        .reason(op.getReason())
                        .employeeFullName(op.getEmployee() != null ? op.getEmployee().getFullName() : null)
                        .batchId(op.getBatch() != null ? op.getBatch().getId() : null)
                        .build()
                )
                .toList();

        return new PageImpl<>(
                content,
                pageable,
                operationsPage.getTotalElements()
        );
    }
}
