package com.shop.system.service.impl;

import com.shop.system.domain.entity.PriceRestriction;
import com.shop.system.domain.entity.UserAccount;
import com.shop.system.domain.enums.PriceRestrictionScopeType;
import com.shop.system.dto.request.CreatePriceRestrictionRequest;
import com.shop.system.dto.request.UpdatePriceRestrictionRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.PriceRestrictionResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.PriceRestrictionRepository;
import com.shop.system.repository.ProductCategoryRepository;
import com.shop.system.repository.ProductRepository;
import com.shop.system.repository.StorageLocationRepository;
import com.shop.system.repository.UserAccountRepository;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.PriceRestrictionService;
import com.shop.system.component.PriceRestrictionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceRestrictionServiceImpl implements PriceRestrictionService {

    private final PriceRestrictionRepository priceRestrictionRepository;
    private final UserAccountRepository userAccountRepository;

    private final StorageLocationRepository storageLocationRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    private final PriceRestrictionValidator validator;

    @Override
    @Transactional(readOnly = true)
    public List<PriceRestrictionResponse> getForCurrentStoreWithGlobals() {
        UUID storeId = resolveCurrentStoreId();

        // если не можем определить ТТ — отдаём только глобальные (storageLocation is null)
        if (storeId == null) {
            return priceRestrictionRepository.findAll().stream()
                    .filter(pr -> pr.getStorageLocation() == null)
                    .sorted((a, b) -> Integer.compare(nullSafe(b.getPriority()), nullSafe(a.getPriority())))
                    .map(this::toResponse)
                    .toList();
        }

        return priceRestrictionRepository.findForStoreWithGlobals(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ApiResponse create(CreatePriceRestrictionRequest request) {
        validator.validate(request);

        PriceRestriction pr = new PriceRestriction();
        applyFromRequest(pr,
                request.getPriority(),
                request.getScopeType(),
                request.getStorageLocationId(),
                request.getProductId(),
                request.getCategoryId(),
                request.getMaxDailyChangePercent(),
                request.getMaxMarkupPercent(),
                request.getAllowDailyChangeExceptionForAutoMarkdown(),
                request.getMinPrice(),
                request.getMaxPrice()
        );

        priceRestrictionRepository.save(pr);
        return new ApiResponse(true, "Правило ограничений цен создано");
    }

    @Override
    @Transactional
    public ApiResponse update(UUID id, UpdatePriceRestrictionRequest request) {
        validator.validate(request);

        PriceRestriction pr = priceRestrictionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Правило не найдено: " + id));

        applyFromRequest(pr,
                request.getPriority(),
                request.getScopeType(),
                request.getStorageLocationId(),
                request.getProductId(),
                request.getCategoryId(),
                request.getMaxDailyChangePercent(),
                request.getMaxMarkupPercent(),
                request.getAllowDailyChangeExceptionForAutoMarkdown(),
                request.getMinPrice(),
                request.getMaxPrice()
        );

        priceRestrictionRepository.save(pr);
        return new ApiResponse(true, "Правило ограничений цен обновлено");
    }

    @Override
    @Transactional
    public ApiResponse delete(UUID id) {
        PriceRestriction pr = priceRestrictionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Правило не найдено: " + id));

        priceRestrictionRepository.delete(pr);
        return new ApiResponse(true, "Правило ограничений цен удалено");
    }

    // ---------------- mapping ----------------

    private PriceRestrictionResponse toResponse(PriceRestriction pr) {
        UUID storageLocationId = pr.getStorageLocation() == null ? null : pr.getStorageLocation().getId();
        UUID productId = pr.getProduct() == null ? null : pr.getProduct().getId();
        UUID categoryId = pr.getCategory() == null ? null : pr.getCategory().getId();

        PriceRestrictionScopeType scopeTypeEnum = null;
        if (pr.getScopeType() != null && !pr.getScopeType().isBlank()) {
            scopeTypeEnum = PriceRestrictionScopeType.valueOf(pr.getScopeType().toUpperCase());
        }

        return PriceRestrictionResponse.builder()
                .id(pr.getId())
                .priority(pr.getPriority())
                .scopeType(scopeTypeEnum)
                .storageLocationId(storageLocationId)
                .productId(productId)
                .categoryId(categoryId)
                .maxDailyChangePercent(pr.getMaxDailyChangePercent())
                .maxMarkupPercent(pr.getMaxMarkupPercent())
                .allowDailyChangeExceptionForAutoMarkdown(pr.getAllowDailyChangeExceptionForAutoMarkdown())
                .minPrice(pr.getMinPrice())
                .maxPrice(pr.getMaxPrice())
                .build();
    }

    private void applyFromRequest(
            PriceRestriction pr,
            Integer priority,
            PriceRestrictionScopeType scopeType,
            UUID storageLocationId,
            UUID productId,
            UUID categoryId,
            java.math.BigDecimal maxDailyChangePercent,
            java.math.BigDecimal maxMarkupPercent,
            Boolean allowException,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice
    ) {
        pr.setPriority(priority == null ? 0 : priority);

        // В БД/Entity хранится lower-case: all/product/category
        pr.setScopeType(scopeType == null ? null : scopeType.name().toLowerCase());

        // NULL => глобальное правило
        pr.setStorageLocation(storageLocationId == null
                ? null
                : storageLocationRepository.findById(storageLocationId)
                .orElseThrow(() -> new BusinessException("ТТ не найдена: " + storageLocationId))
        );

        pr.setProduct(productId == null
                ? null
                : productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Товар не найден: " + productId))
        );

        pr.setCategory(categoryId == null
                ? null
                : productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException("Категория не найдена: " + categoryId))
        );

        pr.setMaxDailyChangePercent(maxDailyChangePercent);
        pr.setMaxMarkupPercent(maxMarkupPercent);
        pr.setAllowDailyChangeExceptionForAutoMarkdown(allowException != null ? allowException : Boolean.FALSE);
        pr.setMinPrice(minPrice);
        pr.setMaxPrice(maxPrice);
    }

    private int nullSafe(Integer v) {
        return v == null ? 0 : v;
    }

    // ---------------- current store resolve ----------------

    private UUID resolveCurrentStoreId() {
        UserAccount ua = getCurrentUserAccount();
        if (ua.getEmployee() == null) return null;

        // типичный путь: employee.storageLocation.id
        if (ua.getEmployee().getStorageLocation() != null) {
            return ua.getEmployee().getStorageLocation().getId();
        }
        return null;
    }

    private UserAccount getCurrentUserAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException("Пользователь не авторизован");
        }

        String login;
        Object principal = auth.getPrincipal();

        if (principal instanceof CurrentUserPrincipal p) {
            login = p.login(); // <-- ВАЖНО: берём login отсюда
        } else {
            login = auth.getName(); // fallback
        }

        return userAccountRepository.findActiveWithDetails(login)
                .orElseThrow(() -> new BusinessException("Активный UserAccount не найден для login=" + login));
    }

}
