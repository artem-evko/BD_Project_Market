package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchForMovementResponse {

    private UUID batchId;
    private UUID productId;
    private String productName;
    private LocalDate expirationDate;
    private BigDecimal totalQuantity;
}
