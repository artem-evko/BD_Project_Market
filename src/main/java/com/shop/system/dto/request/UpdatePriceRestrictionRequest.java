package com.shop.system.dto.request;

import com.shop.system.domain.enums.PriceRestrictionScopeType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UpdatePriceRestrictionRequest {

    private Integer priority;

    private PriceRestrictionScopeType scopeType;

    private UUID storageLocationId;

    private UUID productId;
    private UUID categoryId;

    private BigDecimal maxDailyChangePercent;
    private BigDecimal maxMarkupPercent;

    private Boolean allowDailyChangeExceptionForAutoMarkdown;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
