package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkReportLineOverrideRequest {

    @NotNull
    private UUID lineId;

    /**
     * Дельта, которую директор хочет добавить/убавить вручную (может быть отрицательной).
     * null => снять ручную правку
     */
    private BigDecimal directorOverrideDelta;

    private String overrideReason;
}
