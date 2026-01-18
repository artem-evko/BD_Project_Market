package com.shop.system.service.impl;

import com.shop.system.domain.entity.Batch;
import com.shop.system.domain.entity.BatchLocation;
import com.shop.system.domain.entity.Employee;
import com.shop.system.domain.entity.Inventory;
import com.shop.system.domain.entity.InventoryItem;
import com.shop.system.domain.entity.Product;
import com.shop.system.domain.entity.StorageZone;
import com.shop.system.domain.entity.UserAccount;
import com.shop.system.domain.entity.WarehouseOperation;
import com.shop.system.dto.request.AddInventoryItemRequest;
import com.shop.system.dto.request.CreateInventoryRequest;
import com.shop.system.dto.request.UpdateInventoryItemRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.InventoryDetailsResponse;
import com.shop.system.dto.response.InventoryItemResponse;
import com.shop.system.dto.response.InventoryListItemResponse;
import com.shop.system.exception.BusinessException;
import com.shop.system.repository.BatchLocationRepository;
import com.shop.system.repository.BatchRepository;
import com.shop.system.repository.InventoryItemRepository;
import com.shop.system.repository.InventoryRepository;
import com.shop.system.repository.ProductRepository;
import com.shop.system.repository.StorageZoneRepository;
import com.shop.system.repository.UserAccountRepository;
import com.shop.system.repository.WarehouseOperationRepository;
import com.shop.system.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryItemRepository itemRepository;

    private final UserAccountRepository userAccountRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    private final StorageZoneRepository storageZoneRepository;
    private final BatchLocationRepository batchLocationRepository;
    private final WarehouseOperationRepository warehouseOperationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryListItemResponse> list(String status, LocalDate dateFrom, LocalDate dateTo, int page, int size) {
        UserAccount ua = currentAccount();
        requireAnyInventoryAccess(ua);

        UUID slId = currentStorageLocationId(ua);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        return inventoryRepository.findList(
                        slId,
                        blankToNull(status),
                        dateFrom,
                        dateTo,
                        pageable
                )
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDetailsResponse get(UUID id) {
        UserAccount ua = currentAccount();
        requireAnyInventoryAccess(ua);

        Inventory inv = loadForCurrentSl(id, ua);
        List<InventoryItem> items = itemRepository.findByInventoryIdWithRefs(id);

        return InventoryDetailsResponse.builder()
                .id(inv.getId())
                .inventoryDate(inv.getInventoryDate())
                .status(inv.getStatus())
                .items(items.stream().map(this::toItem).toList())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse create(CreateInventoryRequest request) {
        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        if (request == null || request.getInventoryDate() == null) {
            throw new BusinessException("inventoryDate обязателен");
        }

        Inventory inv = Inventory.builder()
                .inventoryDate(request.getInventoryDate())
                .employee(ua.getEmployee())
                .storageLocation(ua.getEmployee().getStorageLocation())
                .status("PLANNED")
                .build();

        inventoryRepository.save(inv);
        return new ApiResponse(true, "Инвентаризация создана (PLANNED)");
    }

    @Override
    @Transactional
    public ApiResponse start(UUID id) {
        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        Inventory inv = loadForCurrentSl(id, ua);

        if (!"PLANNED".equals(inv.getStatus())) {
            throw new BusinessException("Начать можно только инвентаризацию в статусе PLANNED");
        }

        inv.setStatus("IN_PROGRESS");
        inventoryRepository.save(inv);

        return new ApiResponse(true, "Инвентаризация начата (IN_PROGRESS)");
    }

    @Override
    @Transactional
    public ApiResponse addItem(UUID inventoryId, AddInventoryItemRequest request) {
        UserAccount ua = currentAccount();
        requireParticipantOrDirectorOrAdmin(ua);

        Inventory inv = loadForCurrentSl(inventoryId, ua);

        if (!"IN_PROGRESS".equals(inv.getStatus()) && !"PLANNED".equals(inv.getStatus())) {
            throw new BusinessException("Добавлять позиции можно только в PLANNED/IN_PROGRESS");
        }

        if (request == null || request.getProductId() == null) {
            throw new BusinessException("productId обязателен");
        }
        if (request.getActualQty() == null) {
            throw new BusinessException("actualQty обязателен");
        }
        if (request.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("actualQty должен быть >= 0");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException("Product не найден"));

        Batch batch = null;
        if (request.getBatchId() != null) {
            batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new BusinessException("Batch не найден"));
        }

        // expected можно оставить null (тогда посчитаем при complete),
        // но если прилетело — сохраним.
        BigDecimal expected = request.getExpectedQty();
        BigDecimal actual = request.getActualQty();

        InventoryItem item = InventoryItem.builder()
                .inventory(inv)
                .product(product)
                .batch(batch)
                .expectedQty(expected)
                .actualQty(actual)
                .build();

        itemRepository.save(item);

        return new ApiResponse(true, "Позиция добавлена");
    }

    @Override
    @Transactional
    public ApiResponse updateItem(UUID inventoryId, UUID itemId, UpdateInventoryItemRequest request) {
        UserAccount ua = currentAccount();
        requireParticipantOrDirectorOrAdmin(ua);

        Inventory inv = loadForCurrentSl(inventoryId, ua);

        if (!"IN_PROGRESS".equals(inv.getStatus())) {
            throw new BusinessException("Править факт можно только в IN_PROGRESS");
        }

        if (request == null || request.getActualQty() == null) {
            throw new BusinessException("actualQty обязателен");
        }
        if (request.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("actualQty должен быть >= 0");
        }

        InventoryItem item = itemRepository.findByIdAndInventory_Id(itemId, inventoryId)
                .orElseThrow(() -> new BusinessException("Позиция не найдена"));

        item.setActualQty(request.getActualQty());

        if (request.getExpectedQty() != null) {
            item.setExpectedQty(request.getExpectedQty());
        }

        if (request.getBatchId() != null) {
            Batch batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new BusinessException("Batch не найден"));
            item.setBatch(batch);
        }

        itemRepository.save(item);

        return new ApiResponse(true, "Позиция обновлена");
    }

    @Override
    @Transactional
    public ApiResponse complete(UUID id) {
        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        Inventory inv = loadForCurrentSl(id, ua);

        if (!"IN_PROGRESS".equals(inv.getStatus())) {
            throw new BusinessException("Завершить можно только IN_PROGRESS");
        }

        long cnt = itemRepository.countByInventory_Id(id);
        if (cnt == 0) {
            throw new BusinessException("Нельзя завершить пустую инвентаризацию (без items)");
        }

        UUID slId = inv.getStorageLocation().getId();

        StorageZone defaultZone = storageZoneRepository
                .findFirstByStorageLocation_IdAndIsActiveTrueOrderByZoneTypeAscNameAsc(slId)
                .orElseThrow(() -> new BusinessException("В торговой точке нет активных зон хранения (storage_zones)"));


        List<InventoryItem> items = itemRepository.findByInventoryIdWithRefs(id);

        for (InventoryItem ii : items) {
            if (ii.getActualQty() == null) {
                throw new BusinessException("actualQty обязателен для всех inventory_items");
            }
            if (ii.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("actualQty должен быть >= 0");
            }

            BigDecimal expected = ii.getExpectedQty();
            if (expected == null) {
                if (ii.getBatch() != null) {
                    expected = batchLocationRepository.sumQuantityForBatchInLocation(ii.getBatch().getId(), slId);
                } else {
                    expected = batchLocationRepository.findByProductAndLocation(ii.getProduct().getId(), slId, false)
                            .stream()
                            .map(BatchLocation::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
            }

            BigDecimal actual = ii.getActualQty();
            BigDecimal diff = actual.subtract(expected);

            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // diff > 0 => увеличиваем остатки. Без batch непонятно куда добавлять.
            if (diff.signum() > 0 && ii.getBatch() == null) {
                throw new BusinessException("Для увеличения остатка обязателен batchId (иначе непонятно, в какую партию добавлять)");
            }

            // 1) корректируем остатки batch_locations
            if (ii.getBatch() != null) {
                applyDiffToSingleBatchInZone(ii.getBatch().getId(), defaultZone.getId(), diff);
            } else {
                // сюда попадем только при diff < 0: списываем по продукту из доступных партий
                applyNegativeDiffByProduct(slId, ii.getProduct().getId(), diff);
            }

            // 2) создаем warehouse_operation
            WarehouseOperation op = WarehouseOperation.builder()
                    .type("INVENTORY_ADJUSTMENT")
                    .product(ii.getProduct())
                    .batch(ii.getBatch())
                    .quantity(diff.abs())
                    .operationDate(Instant.now())
                    .employee(ua.getEmployee())
                    .reason("Inventory " + inv.getId() + ": корректировка по позиции " + ii.getId())
                    .sourceDocumentType("INVENTORY")
                    .sourceDocumentId(inv.getId())
                    .fromZone(diff.signum() < 0 ? defaultZone : null)
                    .toZone(diff.signum() > 0 ? defaultZone : null)
                    .build();

            warehouseOperationRepository.save(op);
        }

        inv.setStatus("COMPLETED");
        inventoryRepository.save(inv);

        return new ApiResponse(true, "Инвентаризация завершена (COMPLETED) и остатки скорректированы");
    }

    @Override
    @Transactional
    public ApiResponse cancel(UUID id) {
        UserAccount ua = currentAccount();
        requireDirectorOrAdmin(ua);

        Inventory inv = loadForCurrentSl(id, ua);

        if ("COMPLETED".equals(inv.getStatus())) {
            throw new BusinessException("Нельзя отменить COMPLETED");
        }

        inv.setStatus("CANCELLED");
        inventoryRepository.save(inv);

        return new ApiResponse(true, "Инвентаризация отменена (CANCELLED)");
    }

    // ---------------- mapping ----------------

    private InventoryListItemResponse toListItem(Inventory i) {
        Employee e = i.getEmployee();
        return InventoryListItemResponse.builder()
                .id(i.getId())
                .inventoryDate(i.getInventoryDate())
                .status(i.getStatus())
                .employeeId(e != null ? e.getId() : null)
                .employeeFullName(e != null ? e.getFullName() : null)
                .build();
    }

    private InventoryItemResponse toItem(InventoryItem ii) {
        BigDecimal expected = nz(ii.getExpectedQty());
        BigDecimal actual = nz(ii.getActualQty());

        return InventoryItemResponse.builder()
                .id(ii.getId())
                .productId(ii.getProduct() != null ? ii.getProduct().getId() : null)
                .productName(ii.getProduct() != null ? ii.getProduct().getName() : null)
                .batchId(ii.getBatch() != null ? ii.getBatch().getId() : null)
                .expectedQty(expected)
                .actualQty(actual)
                .diffQty(actual.subtract(expected))
                .build();
    }

    // ---------------- inventory adjustments ----------------

    private void applyDiffToSingleBatchInZone(UUID batchId, UUID zoneId, BigDecimal diff) {
        BatchLocation bl = batchLocationRepository.findByBatchIdAndStorageZoneId(batchId, zoneId)
                .orElseGet(() -> {
                    Batch batch = batchRepository.findById(batchId)
                            .orElseThrow(() -> new BusinessException("Batch не найден: " + batchId));
                    StorageZone zone = storageZoneRepository.findById(zoneId)
                            .orElseThrow(() -> new BusinessException("StorageZone не найдена: " + zoneId));

                    return BatchLocation.builder()
                            .batch(batch)
                            .storageZone(zone)
                            .quantity(BigDecimal.ZERO)
                            .build();
                });

        BigDecimal newQty = nz(bl.getQuantity()).add(diff);
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Недостаточно остатка в партии для списания. batch=" + batchId + ", zone=" + zoneId);
        }

        bl.setQuantity(newQty);
        batchLocationRepository.save(bl);
    }

    private void applyNegativeDiffByProduct(UUID storageLocationId, UUID productId, BigDecimal diff) {
        if (diff.signum() >= 0) return;

        BigDecimal toRemove = diff.abs();

        List<BatchLocation> sources = batchLocationRepository.findByProductAndLocation(productId, storageLocationId, true);
        if (sources.isEmpty()) {
            throw new BusinessException("Нет доступных остатков для списания по продукту: " + productId);
        }

        for (BatchLocation bl : sources) {
            if (toRemove.compareTo(BigDecimal.ZERO) == 0) break;

            BigDecimal canTake = nz(bl.getQuantity());
            if (canTake.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal take = canTake.min(toRemove);

            bl.setQuantity(canTake.subtract(take));
            batchLocationRepository.save(bl);

            toRemove = toRemove.subtract(take);
        }

        if (toRemove.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Недостаточно остатков по продукту для списания. product=" + productId);
        }
    }

    // ---------------- auth/validation ----------------

    private void requireDirectorOrAdmin(UserAccount ua) {
        String code = ua.getRole() != null ? ua.getRole().getCode() : null;
        if (!"DIRECTOR".equals(code) && !"ADMIN".equals(code)) {
            throw new BusinessException("Нет прав: требуется роль DIRECTOR или ADMIN");
        }
    }

    private void requireParticipantOrDirectorOrAdmin(UserAccount ua) {
        String code = ua.getRole() != null ? ua.getRole().getCode() : null;
        if (!Set.of("DIRECTOR", "ADMIN", "STOREKEEPER", "MERCHANDISER").contains(code)) {
            throw new BusinessException("Нет прав: требуется DIRECTOR/ADMIN/STOREKEEPER/MERCHANDISER");
        }
    }

    private void requireAnyInventoryAccess(UserAccount ua) {
        requireParticipantOrDirectorOrAdmin(ua);
    }

    private Inventory loadForCurrentSl(UUID id, UserAccount ua) {
        Inventory inv = inventoryRepository.findWithDetails(id)
                .orElseThrow(() -> new BusinessException("Инвентаризация не найдена"));

        UUID slId = currentStorageLocationId(ua);

        if (!inv.getStorageLocation().getId().equals(slId)) {
            throw new BusinessException("Нет доступа к инвентаризации другой торговой точки");
        }
        return inv;
    }

    private UUID currentStorageLocationId(UserAccount ua) {
        if (ua.getEmployee() == null || ua.getEmployee().getStorageLocation() == null) {
            throw new BusinessException("У пользователя нет привязанной торговой точки (storage_location)");
        }
        return ua.getEmployee().getStorageLocation().getId();
    }

    private UserAccount currentAccount() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new BusinessException("Пользователь не авторизован");
        }

        Object principal = a.getPrincipal();

        String login;
        if (principal instanceof com.shop.system.security.CurrentUserPrincipal p) {
            login = p.login();
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            login = ud.getUsername();
        } else if (principal instanceof String s) {
            login = s;
        } else {
            login = a.getName(); // fallback
        }

        if (login == null || login.isBlank() || "anonymousUser".equalsIgnoreCase(login)) {
            throw new BusinessException("Пользователь не авторизован");
        }

        return userAccountRepository.findByLoginAndIsActiveTrue(login)
                .orElseThrow(() -> new BusinessException("Пользователь не найден или неактивен"));
    }


    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
