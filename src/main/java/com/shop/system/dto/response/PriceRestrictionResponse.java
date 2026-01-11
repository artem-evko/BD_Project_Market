package com.shop.system.dto.response;

import com.shop.system.domain.enums.PriceRestrictionScopeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PriceRestrictionResponse {

    private UUID id;

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
