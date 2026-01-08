package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductLocationBatchResponse {

    private UUID batchId;
    private LocalDate expirationDate;

    private String invoiceNumber;

    private BigDecimal quantity;
}
