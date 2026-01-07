package com.shop.system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    private String name;

    private String barcode;

    private UUID manufacturerId;

    private String unitOfMeasure;

    private Integer shelfLifeDays;

    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    private String additionalInfo;

    private List<UUID> categoryIds;
}
