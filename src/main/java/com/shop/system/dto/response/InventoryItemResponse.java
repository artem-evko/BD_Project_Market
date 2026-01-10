package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class InventoryItemResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID batchId;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal diffQty; // actual - expected
}
