package com.shop.system.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferRequest {

    @NotNull
    private UUID productId;

    @NotNull
    private UUID fromZoneId;

    @NotNull
    private UUID toZoneId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    private String reason;
}
