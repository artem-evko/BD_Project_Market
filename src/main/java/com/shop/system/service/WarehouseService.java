package com.shop.system.service;

import com.shop.system.dto.request.*;
import com.shop.system.dto.response.GoodsReceiptResponse;
import com.shop.system.dto.response.ApiResponse;

import java.util.UUID;

public interface WarehouseService {

    UUID getSupplyInvoiceIdByNumber(String invoiceNumber);

    GoodsReceiptResponse getSupplyInvoiceForReceipt(UUID invoiceId);

    ApiResponse saveReceiptFact(UUID invoiceId, GoodsReceiptRequest request);

    ApiResponse confirmReceipt(UUID invoiceId, GoodsReceiptConfirmRequest request);

    ApiResponse decideDiscrepancy(UUID discrepancyId, DiscrepancyDecisionRequest request);
}
