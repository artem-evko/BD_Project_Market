package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateWorkReportRequest {

    @NotNull
    private LocalDate weekStart; // понедельник
}
