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
public class GoodsReceiptItemResponse {

    private UUID supplyInvoiceItemId;

    private Integer lineNo;

    private UUID productId;
    private String productName;

    private BigDecimal quantityExpected;
    private BigDecimal purchasePrice;

    private BigDecimal quantityActual;

    private LocalDate manufactureDate;
    private LocalDate expirationDate;

    private String lineStatus;

    private UUID batchId;

    private DiscrepancyResponse discrepancy;
}
