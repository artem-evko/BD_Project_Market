package com.shop.system.controller;

import com.shop.system.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
