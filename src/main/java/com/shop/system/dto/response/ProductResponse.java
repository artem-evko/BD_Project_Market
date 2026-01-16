package com.shop.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String barcode;
    private String manufacturer;
    private String category;
    private String unitOfMeasure;
    private Integer shelfLifeDays;
    private BigDecimal currentPrice;
    private BigDecimal totalQuantity;
    private String additionalInfo;
}
