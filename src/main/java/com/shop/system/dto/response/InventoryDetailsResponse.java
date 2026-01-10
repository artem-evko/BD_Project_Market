package com.shop.system.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class InventoryDetailsResponse {
    private UUID id;
    private LocalDate inventoryDate;
    private String status;
    private List<InventoryItemResponse> items;
}
