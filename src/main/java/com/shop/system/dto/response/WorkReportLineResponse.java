package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReportLineResponse {

    private UUID id;

    private UUID employeeId;
    private String employeeFullName;

    private BigDecimal computedHours;
    private BigDecimal normHours;
    private BigDecimal deltaHours;

    private BigDecimal directorOverrideDelta;
    private String overrideReason;
}
