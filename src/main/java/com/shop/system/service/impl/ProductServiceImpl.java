package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.ProductCreateRequest;
import com.shop.system.dto.request.ProductUpdateRequest;
import com.shop.system.dto.response.ProductCategoryResponse;
import com.shop.system.dto.response.ProductDetailResponse;
import com.shop.system.dto.response.ProductResponse;
import com.shop.system.exception.EntityNotFoundException;
import com.shop.system.mapper.ProductMapper;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StorePriceRepository storePriceRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryLinkRepository productCategoryLinkRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProductMapper productMapper;
    private final ManufacturerRepository manufacturerRepository;

    @Override
    public Page<ProductResponse> getProducts(
            int page, int size,
            String search, String category,
            boolean includeArchived) {

        StorageLocation storageLocation = resolveCurrentStorageLocation();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("name").ascending());

        String normalizedSearch = normalize(search);
        String normalizedCategory = normalize(category);
        Page<Product> productPage;
        if(includeArchived) {
            productPage = productRepository.searchProductsIncludingArchived(
                    normalizedSearch,
                    normalizedCategory,
                    pageable
            );
        }
        else{
            productPage = productRepository.searchProducts(
                    normalizedSearch,
                    normalizedCategory,
                    pageable
            );
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        List<ProductResponse> mapped = productPage
                .getContent()
                .stream()
                .map(product -> {
                    BigDecimal currentPrice = resolveCurrentPrice(storageLocation, product, today);
                    return productMapper.toListItem(product, currentPrice);
                })
                .toList();

        return new PageImpl<>(
                mapped,
                pageable,
                productPage.getTotalElements()
        );
    }


    @Override
    public ProductDetailResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product id = {}" + id));
        return productMapper.toDetail(product);
    }

    /**
     * Определяем текущую ТТ из юзера в SecurityContext:
     * UserAccount.login -> Employee -> storageLocation.
     */
    private StorageLocation resolveCurrentStorageLocation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }

        Object principal = auth.getPrincipal();
        String login;

        if (principal instanceof CurrentUserPrincipal cup) {
            login = cup.login();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            login = ud.getUsername();
        } else {
            login = auth.getName();
        }

        log.info("AUTH RESOLVED LOGIN = {}", login);

        UserAccount userAccount = userAccountRepository
                .findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new IllegalStateException("Учётная запись не найдена или не активна"));

        Employee employee = userAccount.getEmployee();
        if (employee == null) {
            throw new IllegalStateException("У учётной записи отсутствует связанный сотрудник");
        }

        StorageLocation storageLocation = employee.getStorageLocation();
        if (storageLocation == null) {
            throw new IllegalStateException(
                    "Для сотрудника '%s' не указана торговая точка / склад"
                            .formatted(employee.getFullName())
            );
        }

        return storageLocation;
    }

    /**
     * Берём последнюю цену на дату для товара по текущей ТТ.
     */
    private BigDecimal resolveCurrentPrice(StorageLocation storageLocation, Product product, LocalDate date) {
        return storePriceRepository
                .findFirstByStorageLocationAndProductAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        storageLocation,
                        product,
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
                            .productCount((int)count)
                            .build();
                })
                .toList();
    }

    @Override
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        var manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new EntityNotFoundException("Manufacturer not found"));

        Product product = productMapper.fromCreateRequest(request, manufacturer);
        Product savedProduct = productRepository.save(product); // отдельная переменная, не трогаем потом

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            var categories = productCategoryRepository.findAllById(request.getCategoryIds());

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
    public ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        var manufacturer = manufacturerRepository.findById(request.getManufacturerId())
                .orElseThrow(() -> new EntityNotFoundException("Manufacturer not found"));

        productMapper.updateEntity(product, request, manufacturer);

        productCategoryLinkRepository.deleteByProduct(product);

        product.getCategoryLinks().clear();

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            var categories = productCategoryRepository.findAllById(request.getCategoryIds());

            List<ProductCategoryLink> links = categories.stream()
                    .map(cat -> ProductCategoryLink.builder()
                            .product(product)
                            .category(cat)
                            .build())
                    .toList();

            productCategoryLinkRepository.saveAll(links);

            product.getCategoryLinks().addAll(links);
        }

        productRepository.save(product);

        return productMapper.toDetail(product);
    }


    @Override
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
    public void unarchiveProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        if (Boolean.FALSE.equals(product.getArchived())) {
            return;
        }
        product.setArchived(false);
        productRepository.save(product);
    }
}


