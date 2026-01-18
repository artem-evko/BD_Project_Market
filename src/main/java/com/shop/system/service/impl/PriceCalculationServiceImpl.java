package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.ManualPriceRequest;
import com.shop.system.dto.response.*;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.PriceCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceCalculationServiceImpl implements PriceCalculationService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final StopListRepository stopListRepository;
    private final PriceListItemRepository priceListItemRepository;
    private final EmployeeRepository employeeRepository;
    private final PriceListRepository priceListRepository;
    private final ProductRepository productRepository;
    private final PriceChangeTaskRepository priceChangeTaskRepository;

    private final StorageLocationRepository storageLocationRepository;

    private final com.shop.system.component.PriceRestrictionResolver priceRestrictionResolver;
    private final com.shop.system.component.PriceRestrictionEnforcer priceRestrictionEnforcer;

    @Override
    @Transactional
    public DailySetupResponse dailySetup(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        if (employee.getStorageLocation() == null) {
            throw new RuntimeException("Employee has no storageLocation: " + employeeId);
        }

        UUID storeId = employee.getStorageLocation().getId();

        Instant startedAt = Instant.now();

        PriceChangeTask task = PriceChangeTask.builder()
                .createdDate(LocalDate.now())
                .createdByEmployee(employee)
                .status("in_progress")
                .startedAt(startedAt)
                .totalProducts(0)
                .pricesUpdated(0)
                .pricesRestricted(0)
                .build();

        task = priceChangeTaskRepository.save(task);

        int processed = 0;
        int updated = 0;
        int restricted = 0;

        List<PriceListItem> items = priceListItemRepository.findAllByStorageLocation_Id(storeId);

        for (PriceListItem item : items) {
            processed++;

            Product product = item.getProduct();
            if (product == null) {
                continue;
            }

            BigDecimal oldPrice = item.getFinalPrice();
            BigDecimal candidatePrice = item.getInputPrice() != null ? item.getInputPrice() : oldPrice;

            if (candidatePrice == null) {
                continue;
            }

            List<UUID> categoryIds = product.getCategoryLinks().stream()
                    .map(ProductCategoryLink::getCategory)
                    .filter(java.util.Objects::nonNull)
                    .map(ProductCategory::getId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();

            boolean isAutoMarkdown = oldPrice != null && candidatePrice.compareTo(oldPrice) < 0;
            BigDecimal baseCost = null;

            PriceRestriction rule = priceRestrictionResolver.resolve(storeId, product.getId(), categoryIds).orElse(null);

            try {
                priceRestrictionEnforcer.enforce(rule, oldPrice, candidatePrice, baseCost, isAutoMarkdown);
            } catch (RuntimeException ex) {
                LocalDate today = LocalDate.now();

                boolean alreadyExists = stopListRepository.existsByStorageLocation_IdAndProduct_IdAndEffectiveDate(
                        storeId, product.getId(), today
                );

                if (!alreadyExists) {
                    StopList sl = StopList.builder()
                            .storageLocation(employee.getStorageLocation())
                            .effectiveDate(today)
                            .task(task)
                            .product(product)
                            .priceListItem(item)
                            .candidatePrice(candidatePrice)
                            .prevPrice(oldPrice)
                            .inputPrice(item.getInputPrice())
                            .violations("{\"priceRestrictions\":true}")
                            .reason(ex.getMessage())
                            .status("pending")
                            .createdAt(LocalDateTime.now())
                            .build();

                    stopListRepository.save(sl);
                }

                restricted++;
                continue;
            }

            if (oldPrice == null || candidatePrice.compareTo(oldPrice) != 0) {
                item.setFinalPrice(candidatePrice);
                if (item.getInputPrice() == null) {
                    item.setInputPrice(candidatePrice);
                }

                if (item.getPriceType() == null || item.getPriceType().isBlank()) {
                    item.setPriceType("REGULAR");
                }

                priceListItemRepository.save(item);

                PriceHistory history = PriceHistory.builder()
                        .product(product)
                        .oldPrice(oldPrice != null ? oldPrice : BigDecimal.ZERO)
                        .newPrice(candidatePrice)
                        .changedBy(employee)
                        .changeDate(Instant.now())
                        .comment("Daily setup")
                        .build();

                priceHistoryRepository.save(history);

                updated++;
            }
        }

        Instant completedAt = Instant.now();

        task.setStatus("completed");
        task.setCompletedAt(completedAt);
        task.setTotalProducts(processed);
        task.setPricesUpdated(updated);
        task.setPricesRestricted(restricted);
        priceChangeTaskRepository.save(task);

        return DailySetupResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .processedProducts(processed)
                .updatedPrices(updated)
                .stopListed(restricted)
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentPriceResponse> getCurrentPrices(UUID storageLocationId) {
        List<PriceListItem> items = priceListItemRepository.findByStorageLocation_Id(storageLocationId);

        return items.stream()
                .map(item -> new CurrentPriceResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getInputPrice(),
                        item.getFinalPrice(),
                        item.getPriceType(),
                        LocalDate.now()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorageLocationResponse> getStorageLocations() {
        // Эквивалент твоего SQL:
        // select id, name from storage_locations order by name;
        return storageLocationRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(sl -> new StorageLocationResponse(sl.getId(), sl.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopListItemResponse> getStopList() {
        return stopListRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(sl -> new StopListItemResponse(
                        sl.getProduct().getId(),
                        sl.getProduct().getName(),
                        sl.getCandidatePrice(),
                        (sl.getReason() != null && !sl.getReason().isBlank()) ? sl.getReason() : sl.getViolations(),
                        sl.getPrevPrice(),
                        sl.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getHistory(UUID productId, Integer days) {
        int safeDays = (days == null || days < 1) ? 30 : days;
        Instant from = Instant.now().minusSeconds((long) safeDays * 24L * 60L * 60L);

        List<PriceHistory> history = priceHistoryRepository
                .findByProductIdAndChangeDateAfterOrderByChangeDateDesc(productId, from);

        return history.stream()
                .map(h -> new PriceHistoryResponse(
                        h.getOldPrice(),
                        h.getNewPrice(),
                        h.getChangeDate(),
                        h.getComment(),
                        (h.getChangedBy() != null && h.getChangedBy().getFullName() != null)
                                ? h.getChangedBy().getFullName()
                                : (h.getChangedBy() != null ? h.getChangedBy().getId().toString() : null)
                ))
                .toList();
    }

    @Transactional
    @Override
    public ManualPriceResponse manualChange(ManualPriceRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new RuntimeException("Unauthorized: principal not found");
        }

        UUID employeeId = principal.employeeId();

        Employee employee = employeeRepository.findWithDetailsById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.productId()));

        PriceListItem item = priceListItemRepository
                .findTopByProduct_IdOrderByPriceList_EffectiveDateDesc(product.getId())
                .orElse(null);

        if (item == null) {
            PriceList priceList = priceListRepository.findTopByOrderByEffectiveDateDesc()
                    .orElseThrow(() -> new RuntimeException("No price_lists found. Seed price_lists first."));

            item = PriceListItem.builder()
                    .priceList(priceList)
                    .product(product)
                    .storageLocation(priceList.getStorageLocation())
                    .priceType("REGULAR")
                    .build();
        }

        BigDecimal oldPrice = item.getFinalPrice();
        BigDecimal newPrice = request.price();

        UUID storeId = (item.getStorageLocation() != null) ? item.getStorageLocation().getId() : null;

        List<UUID> categoryIds = product.getCategoryLinks().stream()
                .map(ProductCategoryLink::getCategory)
                .filter(java.util.Objects::nonNull)
                .map(ProductCategory::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        BigDecimal baseCost = null;
        boolean isAutoMarkdown = false;

        PriceRestriction rule = priceRestrictionResolver
                .resolve(storeId, product.getId(), categoryIds)
                .orElse(null);

        priceRestrictionEnforcer.enforce(rule, oldPrice, newPrice, baseCost, isAutoMarkdown);

        item.setFinalPrice(newPrice);
        item.setInputPrice(newPrice);

        if (item.getPriceType() == null || item.getPriceType().isBlank()) {
            item.setPriceType("REGULAR");
        }

        priceListItemRepository.save(item);

        PriceHistory history = PriceHistory.builder()
                .product(product)
                .oldPrice(oldPrice != null ? oldPrice : BigDecimal.ZERO)
                .newPrice(newPrice)
                .changedBy(employee)
                .changeDate(Instant.now())
                .comment(request.reason())
                .build();

        priceHistoryRepository.save(history);

        LocalDate effectiveDate =
                (item.getPriceList() != null && item.getPriceList().getEffectiveDate() != null)
                        ? item.getPriceList().getEffectiveDate()
                        : LocalDate.now();

        return new ManualPriceResponse(
                product.getId(),
                product.getName(),
                oldPrice,
                newPrice,
                effectiveDate
        );
    }
}
