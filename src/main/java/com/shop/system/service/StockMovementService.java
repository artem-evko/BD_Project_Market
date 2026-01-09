package com.shop.system.service;

import com.shop.system.dto.request.StockTransferRequest;
import com.shop.system.dto.response.BatchForMovementResponse;
import com.shop.system.dto.response.StorageZoneResponse;
import com.shop.system.dto.response.WarehouseOperationMovementResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StockMovementService {

    List<StorageZoneResponse> getStorageZonesForCurrentLocation();

    List<BatchForMovementResponse> getBatchesForProduct(UUID productId, Boolean onlyAvailable);

    WarehouseOperationMovementResponse createTransfer(StockTransferRequest request);

    Page<WarehouseOperationMovementResponse> getOperations(
            String type,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    );
}
