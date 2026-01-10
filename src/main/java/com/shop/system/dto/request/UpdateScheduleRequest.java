package com.shop.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpdateScheduleRequest {
    private LocalTime plannedStart;
    private LocalTime plannedEnd;

    /**
     * regular/overtime/day_off/short_day
     */
    private String scheduleType;

    @NotBlank
    private String correctionReason;
}
