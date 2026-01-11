package com.shop.system.component;

import com.shop.system.domain.entity.PriceRestriction;
import com.shop.system.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PriceRestrictionEnforcer {

    public void enforce(
            PriceRestriction rule,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            BigDecimal baseCost,
            boolean isAutoMarkdown
    ) {
        if (rule == null) return;

        if (newPrice == null) throw new BusinessException("Новая цена не задана");
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("Цена не может быть отрицательной");

        // min/max
        if (rule.getMinPrice() != null && newPrice.compareTo(rule.getMinPrice()) < 0) {
            throw new BusinessException("Цена ниже минимально допустимой: " + rule.getMinPrice());
        }
        if (rule.getMaxPrice() != null && newPrice.compareTo(rule.getMaxPrice()) > 0) {
            throw new BusinessException("Цена выше максимально допустимой: " + rule.getMaxPrice());
        }

        // дневное изменение (%)
        // если oldPrice null/0 — пропускаем (или решим отдельно)
        if (rule.getMaxDailyChangePercent() != null && oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {

            // если автоуценка и разрешено исключение — пропускаем лимит дневного изменения
            if (!(isAutoMarkdown && Boolean.TRUE.equals(rule.getAllowDailyChangeExceptionForAutoMarkdown()))) {

                BigDecimal diff = newPrice.subtract(oldPrice).abs();
                BigDecimal percent = diff
                        .divide(oldPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if (percent.compareTo(rule.getMaxDailyChangePercent()) > 0) {
                    throw new BusinessException("Превышен лимит дневного изменения цены: "
                            + rule.getMaxDailyChangePercent() + "% (факт: " + percent.setScale(2, RoundingMode.HALF_UP) + "%)");
                }
            }
        }

        // наценка (%)
        // markup = (newPrice - baseCost) / baseCost * 100
        if (rule.getMaxMarkupPercent() != null
                && baseCost != null
                && baseCost.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal markup = newPrice.subtract(baseCost)
                    .divide(baseCost, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (markup.compareTo(rule.getMaxMarkupPercent()) > 0) {
                throw new BusinessException("Превышен лимит наценки: "
                        + rule.getMaxMarkupPercent() + "% (факт: " + markup.setScale(2, RoundingMode.HALF_UP) + "%)");
            }
        }
    }
}