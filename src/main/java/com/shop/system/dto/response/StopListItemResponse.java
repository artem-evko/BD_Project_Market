package com.shop.system.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StopListItemResponse(
        UUID productId,
        String productName,
        BigDecimal proposedPrice,
        String violationRule,
        BigDecimal currentPrice,
        LocalDateTime createdAt
) {}
