package com.shop.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdateInventoryItemRequest {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal actualQty;

    private BigDecimal expectedQty;
    private UUID batchId;
}
