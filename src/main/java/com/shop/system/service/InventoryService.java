package com.shop.system.service;

import com.shop.system.dto.request.AddInventoryItemRequest;
import com.shop.system.dto.request.CreateInventoryRequest;
import com.shop.system.dto.request.UpdateInventoryItemRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.InventoryDetailsResponse;
import com.shop.system.dto.response.InventoryListItemResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface InventoryService {

    Page<InventoryListItemResponse> list(
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size
    );

    InventoryDetailsResponse get(UUID id);

    ApiResponse create(CreateInventoryRequest request);

    ApiResponse start(UUID id);

    ApiResponse addItem(UUID inventoryId, AddInventoryItemRequest request);

    ApiResponse updateItem(UUID inventoryId, UUID itemId, UpdateInventoryItemRequest request);

    ApiResponse complete(UUID id);

    ApiResponse cancel(UUID id);
}
