package com.shop.system.component;

import com.shop.system.domain.enums.PriceRestrictionScopeType;
import com.shop.system.dto.request.CreatePriceRestrictionRequest;
import com.shop.system.dto.request.UpdatePriceRestrictionRequest;
import com.shop.system.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceRestrictionValidator {

    public void validate(CreatePriceRestrictionRequest r) {
        validateCommon(r.getScopeType(), r.getProductId(), r.getCategoryId(),
                r.getMaxDailyChangePercent(), r.getMaxMarkupPercent(), r.getMinPrice(), r.getMaxPrice());
    }

    public void validate(UpdatePriceRestrictionRequest r) {
        validateCommon(r.getScopeType(), r.getProductId(), r.getCategoryId(),
                r.getMaxDailyChangePercent(), r.getMaxMarkupPercent(), r.getMinPrice(), r.getMaxPrice());
    }

    private void validateCommon(
            PriceRestrictionScopeType scopeType,
            java.util.UUID productId,
            java.util.UUID categoryId,
            BigDecimal maxDailyChangePercent,
            BigDecimal maxMarkupPercent,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        if (scopeType == null) throw new ValidationException("scopeType обязателен");

        switch (scopeType) {
            case ALL -> {
                if (productId != null || categoryId != null)
                    throw new ValidationException("Для scopeType=ALL productId/categoryId должны быть пустыми");
            }
            case PRODUCT -> {
                if (productId == null)
                    throw new ValidationException("Для scopeType=PRODUCT productId обязателен");
            }
            case CATEGORY -> {
                if (categoryId == null)
                    throw new ValidationException("Для scopeType=CATEGORY categoryId обязателен");
            }
        }

        if (maxDailyChangePercent != null && maxDailyChangePercent.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("maxDailyChangePercent не может быть отрицательным");

        if (maxMarkupPercent != null && maxMarkupPercent.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("maxMarkupPercent не может быть отрицательным");

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("minPrice не может быть отрицательным");

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new ValidationException("maxPrice не может быть отрицательным");

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)
            throw new ValidationException("minPrice не может быть больше maxPrice");
    }
}
