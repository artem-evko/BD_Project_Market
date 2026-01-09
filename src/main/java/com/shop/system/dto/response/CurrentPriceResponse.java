package com.shop.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CurrentPriceResponse(
        UUID productId,
        String productName,
        BigDecimal inputPrice,
        BigDecimal finalPrice,
        String priceType,
        LocalDate effectiveDate
) {}
