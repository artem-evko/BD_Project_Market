package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOperationResponse {

    private UUID id;
    private Instant operationDate;
    private String type;
    private BigDecimal quantity;

    private String fromZone;
    private String toZone;

    private String reason;
    private String employeeFullName;

    private UUID batchId;
}
