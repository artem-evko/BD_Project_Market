package com.shop.system.controller;

import com.shop.system.dto.request.StockTransferRequest;
import com.shop.system.dto.response.BatchForMovementResponse;
import com.shop.system.dto.response.StorageZoneResponse;
import com.shop.system.dto.response.WarehouseOperationMovementResponse;
import com.shop.system.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    /**
     * GET /api/storage-zones
     * Зоны текущей ТТ
     */
    @GetMapping("/storage-zones")
    public List<StorageZoneResponse> getStorageZones() {
        return stockMovementService.getStorageZonesForCurrentLocation();
    }

    /**
     * GET /api/batches?productId=&onlyAvailable=true
     * Партии товара по текущей ТТ
     */
    @GetMapping("/batches")
    public List<BatchForMovementResponse> getBatches(
            @RequestParam("productId") UUID productId,
            @RequestParam(value = "onlyAvailable", required = false, defaultValue = "true") Boolean onlyAvailable
    ) {
        return stockMovementService.getBatchesForProduct(productId, onlyAvailable);
    }

    /**
     * POST /api/warehouse-operations/transfer
     * Создать перемещение (TRANSFER)
     */
    @PostMapping("/warehouse-operations/transfer")
    public WarehouseOperationMovementResponse createTransfer(
            @Valid @RequestBody StockTransferRequest request
    ) {
        return stockMovementService.createTransfer(request);
    }

    /**
     * GET /api/warehouse-operations?type=transfer&dateFrom=&dateTo=&page=&size=
     * История операций по текущей ТТ (для страницы Перемещений)
     */
    @GetMapping("/warehouse-operations")
    public Page<WarehouseOperationMovementResponse> getOperations(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        return stockMovementService.getOperations(type, dateFrom, dateTo, page, size);
    }
}
