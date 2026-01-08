package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLocationZoneResponse {
    private UUID zoneId;
    private String storageZoneName;
    private String zoneType;
    private String temperatureMode;
    private BigDecimal zoneTotalQty;
    private List<ProductLocationBatchResponse> batches;
}
