package com.shop.system.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySetupResponse {
    private UUID taskId;
    private String status;
    private Integer processedProducts;
    private Integer updatedPrices;
    private Integer stopListed;
    private Instant startedAt;
    private Instant completedAt;
}
