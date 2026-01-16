package com.shop.system.dto.response;

import com.shop.system.dto.ManufacturerDto;
import com.shop.system.dto.DimensionsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {
    private UUID id;
    private String name;
    private String barcode;
    private String category;
    private String unitOfMeasure;
    private Integer shelfLifeDays;
    private DimensionsDto dimensions;
    private ManufacturerDto manufacturer;
    private BigDecimal totalQuantity;
    private String additionalInfo;
}
