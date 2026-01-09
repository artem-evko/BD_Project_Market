package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ManualPriceRequest(
        @NotNull UUID productId,
        @NotNull @Positive BigDecimal price,
        @Size(max = 500) String reason
) {}
