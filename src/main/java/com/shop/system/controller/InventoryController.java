package com.shop.system.controller;

import com.shop.system.dto.request.AddInventoryItemRequest;
import com.shop.system.dto.request.CreateInventoryRequest;
import com.shop.system.dto.request.UpdateInventoryItemRequest;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.InventoryDetailsResponse;
import com.shop.system.dto.response.InventoryListItemResponse;
import com.shop.system.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public Page<InventoryListItemResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return inventoryService.list(status, dateFrom, dateTo, page, size);
    }

    @GetMapping("/{id}")
    public InventoryDetailsResponse get(@PathVariable UUID id) {
        return inventoryService.get(id);
    }

    @PostMapping
    public ApiResponse create(@RequestBody @Valid CreateInventoryRequest req) {
        return inventoryService.create(req);
    }

    @PostMapping("/{id}/start")
    public ApiResponse start(@PathVariable UUID id) {
        return inventoryService.start(id);
    }

    @PostMapping("/{id}/items")
    public ApiResponse addItem(@PathVariable UUID id, @RequestBody @Valid AddInventoryItemRequest req) {
        return inventoryService.addItem(id, req);
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse updateItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateInventoryItemRequest req
    ) {
        return inventoryService.updateItem(id, itemId, req);
    }

    @PostMapping("/{id}/complete")
    public ApiResponse complete(@PathVariable UUID id) {
        return inventoryService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse cancel(@PathVariable UUID id) {
        return inventoryService.cancel(id);
    }
}
