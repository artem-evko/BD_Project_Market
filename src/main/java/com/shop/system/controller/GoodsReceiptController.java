package com.shop.system.controller;

import com.shop.system.dto.request.*;
import com.shop.system.dto.response.ApiResponse;
import com.shop.system.dto.response.GoodsReceiptResponse;
import com.shop.system.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse/goods-receipt")
public class GoodsReceiptController {

    private final WarehouseService warehouseService;

    /**
     * Получить накладную + строки для приёмки
     */
    @GetMapping("/{invoiceId}")
    public GoodsReceiptResponse getForReceipt(@PathVariable UUID invoiceId) {
        return warehouseService.getSupplyInvoiceForReceipt(invoiceId);
    }

    /**
     * Сохранить факт (кол-во факт, даты, признак несоответствия)
     */
    @PostMapping("/{invoiceId}/fact")
    public ApiResponse saveFact(@PathVariable UUID invoiceId,
                                @Valid @RequestBody GoodsReceiptRequest request) {
        return warehouseService.saveReceiptFact(invoiceId, request);
    }

    /**
     * Подтвердить приёмку
     */
    @PostMapping("/{invoiceId}/confirm")
    public ApiResponse confirm(@PathVariable UUID invoiceId,
                               @RequestBody GoodsReceiptConfirmRequest request) {
        return warehouseService.confirmReceipt(invoiceId, request);
    }

    /**
     * Решение по несоответствию (товаровед)
     */
    @PostMapping("/discrepancies/{discrepancyId}/decision")
    public ApiResponse decideDiscrepancy(@PathVariable UUID discrepancyId,
                                         @Valid @RequestBody DiscrepancyDecisionRequest request) {
        return warehouseService.decideDiscrepancy(discrepancyId, request);
    }
}