package com.shop.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ManualPriceResponse(
        UUID productId,
        String productName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        LocalDate effectiveDate
) {}
