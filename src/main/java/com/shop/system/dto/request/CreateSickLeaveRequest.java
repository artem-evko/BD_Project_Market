package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSickLeaveRequest {

    @NotNull(message = "dateStart обязателен")
    private LocalDate dateStart;

    @NotNull(message = "dateEnd обязателен")
    private LocalDate dateEnd;
}
