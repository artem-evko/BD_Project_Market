package com.shop.system.controller;

import com.shop.system.dto.response.SupplyInvoiceListItemResponse;
import com.shop.system.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * Поиск накладной по номеру (возвращаем invoiceId, чтобы фронт открыл приёмку)
     */
    @GetMapping("/supply-invoices/by-number")
    public UUID getSupplyInvoiceIdByNumber(@RequestParam String invoiceNumber) {
        return warehouseService.getSupplyInvoiceIdByNumber(invoiceNumber);
    }

    @GetMapping("/supply-invoices")
    public Page<SupplyInvoiceListItemResponse> listSupplyInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID storageLocationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedTo,
            Pageable pageable
    ) {
        return warehouseService.listSupplyInvoices(search, status, storageLocationId, expectedFrom, expectedTo, pageable);
    }
}
