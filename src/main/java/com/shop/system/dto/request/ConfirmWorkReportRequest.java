package com.shop.system.dto.request;

import jakarta.validation.Valid;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmWorkReportRequest {

    /**
     * Переопределения по строкам отчёта.
     * Если directorOverrideDelta != null => overrideReason обязателен
     */
    @Valid
    @Builder.Default
    private List<WorkReportLineOverrideRequest> overrides = new ArrayList<>();
}
