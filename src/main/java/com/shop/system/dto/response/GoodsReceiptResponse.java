package com.shop.system.dto.response;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponse {

    private UUID supplyInvoiceId;

    private String invoiceNumber;
    private String status;

    private UUID contractId;
    private String contractNumber;

    private UUID storageLocationId;

    private LocalDate expectedDate;
    private LocalDate actualDate;

    private UUID receivedZoneId;

    private UUID storekeeperId;
    private UUID merchandiserId;

    private Instant createdAt;
    private Instant updatedAt;

    private List<GoodsReceiptItemResponse> items;
}
