package com.shop.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddInventoryItemRequest {
    @NotNull
    private UUID productId;

    private UUID batchId; // optional, но может стать обязательным

    private BigDecimal expectedQty; // можно null -> посчитаем/проставим 0
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal actualQty;
}
