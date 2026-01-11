package com.shop.system.dto.request;

import com.shop.system.domain.enums.PriceRestrictionScopeType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePriceRestrictionRequest {

    private Integer priority;

    private PriceRestrictionScopeType scopeType;

    // null => глобальное правило, иначе правило для ТТ
    private UUID storageLocationId;

    // обязательны в зависимости от scopeType
    private UUID productId;
    private UUID categoryId;

    private BigDecimal maxDailyChangePercent;
    private BigDecimal maxMarkupPercent;

    private Boolean allowDailyChangeExceptionForAutoMarkdown;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
