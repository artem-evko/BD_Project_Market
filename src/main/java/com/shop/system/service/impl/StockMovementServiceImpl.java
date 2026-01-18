package com.shop.system.service.impl;

import com.shop.system.domain.entity.*;
import com.shop.system.dto.request.StockTransferRequest;
import com.shop.system.dto.response.BatchForMovementResponse;
import com.shop.system.dto.response.StorageZoneResponse;
import com.shop.system.dto.response.WarehouseOperationMovementResponse;
import com.shop.system.exception.EntityNotFoundException;
import com.shop.system.repository.*;
import com.shop.system.security.CurrentUserPrincipal;
import com.shop.system.service.StockMovementService;
import com.shop.system.service.context.StorageLocationContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockMovementServiceImpl implements StockMovementService {

    private final StorageLocationContextService storageLocationContextService;
    private final StorageZoneRepository storageZoneRepository;
    private final BatchLocationRepository batchLocationRepository;
    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final WarehouseOperationRepository warehouseOperationRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public List<StorageZoneResponse> getStorageZonesForCurrentLocation() {
        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        List<StorageZone> zones = storageZoneRepository
                .findByStorageLocationIdAndIsActiveTrueOrderByNameAsc(storageLocation.getId());

        return zones.stream()
                .map(z -> StorageZoneResponse.builder()
                        .id(z.getId())
                        .name(z.getName())
                        .zoneType(z.getZoneType())
                        .temperatureMode(z.getTemperatureMode())
                        .capacity(z.getCapacity())
                        .active(z.getIsActive())
                        .build()
                )
                .toList();
    }

    @Override
    public List<BatchForMovementResponse> getBatchesForProduct(UUID productId, Boolean onlyAvailable) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }

        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();
        boolean onlyAvailableEffective = (onlyAvailable == null) || Boolean.TRUE.equals(onlyAvailable);

        log.info("GET BATCHES FOR MOVEMENTS: productId={}, storageLocationId={}, onlyAvailable={}",
                productId, storageLocation.getId(), onlyAvailableEffective);

        List<BatchLocation> locations = batchLocationRepository.findByProductAndLocation(
                productId,
                storageLocation.getId(),
                onlyAvailableEffective
        );

        // группируем по партии, суммируем остаток по всем зонам
        Map<Batch, BigDecimal> byBatch = locations.stream()
                .collect(Collectors.groupingBy(
                        BatchLocation::getBatch,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                bl -> bl.getQuantity() == null ? BigDecimal.ZERO : bl.getQuantity(),
                                BigDecimal::add
                        )
                ));

        // подгрузим продукт (чтобы не лезть лениво в каждом батче)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        return byBatch.entrySet().stream()
                .map(e -> {
                    Batch batch = e.getKey();
                    BigDecimal totalQty = e.getValue();

                    return BatchForMovementResponse.builder()
                            .batchId(batch.getId())
                            .productId(product.getId())
                            .productName(product.getName())
                            .expirationDate(batch.getExpirationDate())
                            .totalQuantity(totalQty)
                            .build();
                })
                .sorted(Comparator.comparing(
                        b -> Optional.ofNullable(b.getExpirationDate())
                                .orElse(LocalDate.of(3000, 1, 1))
                ))
                .toList();
    }

    @Override
    @Transactional
    public WarehouseOperationMovementResponse createTransfer(StockTransferRequest request) {
        if (request.getFromZoneId().equals(request.getToZoneId())) {
            throw new IllegalArgumentException("from_zone и to_zone не могут совпадать");
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity должен быть > 0");
        }

        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();

        StorageZone fromZone = storageZoneRepository.findById(request.getFromZoneId())
                .orElseThrow(() -> new EntityNotFoundException("from_zone not found: " + request.getFromZoneId()));

        StorageZone toZone = storageZoneRepository.findById(request.getToZoneId())
                .orElseThrow(() -> new EntityNotFoundException("to_zone not found: " + request.getToZoneId()));

        // контролируем, что обе зоны внутри текущей ТТ
        if (!fromZone.getStorageLocation().getId().equals(storageLocation.getId())
                || !toZone.getStorageLocation().getId().equals(storageLocation.getId())) {
            throw new IllegalArgumentException("Зоны должны принадлежать текущей торговой точке");
        }

        // ИЩЕМ ПАРТИЮ ПО productId + fromZone + текущая ТТ
        BatchLocation fromLocation = batchLocationRepository
                .findTop1ByBatch_Product_IdAndStorageZone_IdAndStorageZone_StorageLocation_IdAndQuantityGreaterThanOrderByBatch_ExpirationDateAsc(
                        request.getProductId(),
                        request.getFromZoneId(),
                        storageLocation.getId(),
                        BigDecimal.ZERO
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Для товара " + request.getProductId()
                                + " нет партий с положительным остатком в зоне " + fromZone.getName()
                ));

        Batch batch = fromLocation.getBatch();
        Product product = batch.getProduct();

        // на всякий случай, если кто-то руками шлёт кривой productId
        if (!product.getId().equals(request.getProductId())) {
            throw new IllegalStateException("Найдена партия, но её productId не совпадает с request.productId. Это уже какая-то жопа в данных.");
        }

        BigDecimal availableInFromZone = fromLocation.getQuantity() == null
                ? BigDecimal.ZERO
                : fromLocation.getQuantity();

        if (availableInFromZone.compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    "Недостаточно остатка в зоне '%s': доступно=%s, запрошено=%s"
                            .formatted(fromZone.getName(), availableInFromZone, request.getQuantity())
            );
        }

        // списываем из fromZone
        BigDecimal newFromQty = availableInFromZone.subtract(request.getQuantity());
        fromLocation.setQuantity(newFromQty);

        // ищем / создаём запись для toZone по этой же партии
        BatchLocation toLocation = batchLocationRepository
                .findByBatchIdAndStorageZoneId(batch.getId(), request.getToZoneId())
                .orElseGet(() -> BatchLocation.builder()
                        .batch(batch)
                        .storageZone(toZone)
                        .quantity(BigDecimal.ZERO)
                        .build()
                );

        BigDecimal currentToQty = toLocation.getQuantity() == null
                ? BigDecimal.ZERO
                : toLocation.getQuantity();

        BigDecimal newToQty = currentToQty.add(request.getQuantity());
        toLocation.setQuantity(newToQty);

        batchLocationRepository.save(fromLocation);
        batchLocationRepository.save(toLocation);

        // записываем операцию склада
        Employee employee = resolveCurrentEmployee();

        WarehouseOperation operation = WarehouseOperation.builder()
                .type("TRANSFER")
                .product(product)
                .fromZone(fromZone)
                .toZone(toZone)
                .quantity(request.getQuantity())
                .operationDate(Instant.now())
                .employee(employee)
                .reason(request.getReason())
                .batch(batch)
                .sourceDocumentType("INTERNAL_TRANSFER")
                .sourceDocumentId(null)
                .build();

        WarehouseOperation saved = warehouseOperationRepository.save(operation);

        return WarehouseOperationMovementResponse.builder()
                .id(saved.getId())
                .operationDate(saved.getOperationDate())
                .type(saved.getType())
                .productId(product.getId())
                .productName(product.getName())
                .quantity(saved.getQuantity())
                .fromZone(fromZone.getName())
                .toZone(toZone.getName())
                .reason(saved.getReason())
                .employeeFullName(employee.getFullName())
                .batchId(batch.getId())   // всё ещё отдаём, просто не требуем от фронта
                .build();
    }


    @Override
    public Page<WarehouseOperationMovementResponse> getOperations(
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    ) {
        StorageLocation storageLocation = storageLocationContextService.getCurrentStorageLocation();
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate effectiveFrom = dateFrom != null ? dateFrom : LocalDate.of(1970, 1, 1);
        LocalDate effectiveTo   = dateTo   != null ? dateTo   : LocalDate.of(3000, 1, 1);

        Instant fromInstant = effectiveFrom.atStartOfDay(zoneId).toInstant();
        Instant toInstant   = effectiveTo.plusDays(1).atStartOfDay(zoneId).toInstant();

        log.info("GET WAREHOUSE OPERATIONS (movements): storageLocationId={}, type='{}', from={}, to={}, page={}, size={}",
                storageLocation.getId(), type, fromInstant, toInstant, page, size);

        List<WarehouseOperation> allInPeriod = warehouseOperationRepository.findForLocationInPeriod(
                storageLocation.getId(),
                fromInstant,
                toInstant
        );

        String normalizedType = type != null ? type.trim().toUpperCase(Locale.ROOT) : null;

        List<WarehouseOperation> filtered = allInPeriod.stream()
                .filter(op -> normalizedType == null || normalizedType.equalsIgnoreCase(op.getType()))
                .toList();

        // ручная пагинация, чтобы не городить динамический JPQL
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operationDate"));

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<WarehouseOperationMovementResponse> content = filtered.subList(fromIndex, toIndex).stream()
                .map(op -> WarehouseOperationMovementResponse.builder()
                        .id(op.getId())
                        .operationDate(op.getOperationDate())
                        .type(op.getType())
                        .productId(op.getProduct() != null ? op.getProduct().getId() : null)
                        .productName(op.getProduct() != null ? op.getProduct().getName() : null)
                        .quantity(op.getQuantity())
                        .fromZone(op.getFromZone() != null ? op.getFromZone().getName() : null)
                        .toZone(op.getToZone() != null ? op.getToZone().getName() : null)
                        .reason(op.getReason())
                        .employeeFullName(op.getEmployee() != null ? op.getEmployee().getFullName() : null)
                        .batchId(op.getBatch() != null ? op.getBatch().getId() : null)
                        .build()
                )
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private Employee resolveCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CurrentUserPrincipal cup)) {
            throw new IllegalStateException("Ожидался CurrentUserPrincipal, а пришло: " + principal);
        }

        UUID employeeId = cup.employeeId();
        if (employeeId == null) {
            throw new IllegalStateException("В токене отсутствует employeeId");
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Сотрудник не найден: " + employeeId));
    }
}
