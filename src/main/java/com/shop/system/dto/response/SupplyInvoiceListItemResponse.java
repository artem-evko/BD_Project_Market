package com.shop.system.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyInvoiceListItemResponse {

    private UUID id;

    private String invoiceNumber;
    private String status;

    private LocalDate expectedDate;
    private LocalDate actualDate;

    private UUID storageLocationId;

    private UUID contractId;
    private String contractNumber;
}
