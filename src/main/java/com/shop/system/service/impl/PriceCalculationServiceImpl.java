package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.ManualPriceRequest;
import com.shop.system.dto.response.*;
import com.shop.system.repository.*;
import com.shop.system.service.PriceCalculationService;
import com.shop.system.dto.response.ManualPriceResponse;
import com.shop.system.security.CurrentUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


import java.math.BigDecimal;

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


    @Override
    public DailySetupResponse dailySetup(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        Instant now = Instant.now();
        int processed = (int) productRepository.count();
        int stopListed = (int) stopListRepository.count(); // если хочешь — позже ограничим по дате/локации

        PriceChangeTask task = PriceChangeTask.builder()
                .createdDate(LocalDate.now())
                .createdByEmployee(employee)
                .status("completed")
                .startedAt(now)
                .completedAt(now)
                .totalProducts(processed)
                .pricesUpdated(0)
                .pricesRestricted(stopListed)
                .build();

        task = priceChangeTaskRepository.save(task);

        return DailySetupResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .processedProducts(processed)
                .updatedPrices(task.getPricesUpdated())
                .stopListed(stopListed)
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
                        LocalDate.now() // как было у тебя, позже заменим на PriceList.effectiveDate
                ))
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

        // 1) current user (из JWT)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new RuntimeException("Unauthorized: principal not found");
        }

        UUID employeeId = principal.employeeId();

        Employee employee = employeeRepository.findWithDetailsById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // 2) продукт
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.productId()));

        // 3) ищем item (самый свежий по прайс-листу)
        PriceListItem item = priceListItemRepository
                .findTopByProduct_IdOrderByPriceList_EffectiveDateDesc(product.getId())
                .orElse(null);

        // 4) если нет item — создаём на базе последнего прайс-листа
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

        // 5) обновляем цену
        item.setFinalPrice(newPrice);

        // трактуем ручную цену как “итоговую”, input можно оставить как было — но для простоты синхронизируем
        item.setInputPrice(newPrice);

        if (item.getPriceType() == null || item.getPriceType().isBlank()) {
            item.setPriceType("REGULAR");
        }

        priceListItemRepository.save(item);

        // 6) история
        PriceHistory history = PriceHistory.builder()
                .product(product)
                .oldPrice(oldPrice != null ? oldPrice : BigDecimal.ZERO)
                .newPrice(newPrice)
                .changedBy(employee)
                .changeDate(Instant.now())
                .comment(request.reason())
                .build();

        priceHistoryRepository.save(history);

        // 7) effectiveDate
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
