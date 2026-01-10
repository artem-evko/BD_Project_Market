package com.shop.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyDecisionRequest {

    /**
     * approve / reject
     */
    @NotBlank
    private String decision;

    /**
     * Для reject — обязателен (проверим в сервисе).
     */
    private String comment;
}
