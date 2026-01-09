package com.shop.system.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryResponse(
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Instant changeDate,
        String reason,
        String changedBy
) {}
