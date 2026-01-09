package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseOperationMovementResponse {

    private UUID id;
    private Instant operationDate;
    private String type;

    private UUID productId;
    private String productName;

    private BigDecimal quantity;

    private String fromZone;
    private String toZone;

    private String reason;

    private String employeeFullName;

    private UUID batchId;
}
