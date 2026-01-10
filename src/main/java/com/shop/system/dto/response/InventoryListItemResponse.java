package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
public class InventoryListItemResponse {
    private UUID id;
    private LocalDate inventoryDate;
    private String status;
    private UUID employeeId;
    private String employeeFullName;
}
