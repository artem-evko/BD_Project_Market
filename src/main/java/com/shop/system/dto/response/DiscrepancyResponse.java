package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyResponse {

    private UUID id;

    private String discrepancyType;
    private String description;

    private BigDecimal quantityExpected;
    private BigDecimal quantityActual;

    private String status;

    private String decisionComment;
    private Instant decisionAt;

    private String hqDecisionComment;
    private Instant hqDecisionAt;
}
