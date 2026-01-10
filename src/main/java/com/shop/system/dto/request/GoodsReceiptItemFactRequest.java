package com.shop.system.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItemFactRequest {

    @NotNull
    private UUID supplyInvoiceItemId;

    private BigDecimal quantityActual;

    private LocalDate manufactureDate;

    private LocalDate expirationDate;

    /**
     * Если true — считаем, что нужно создать/обновить discrepancy_request.
     * Если false — discrepancy (если был) можно будет закрывать/удалять по логике сервиса (сделаем в шаге 3).
     */
    private Boolean hasDiscrepancy;

    /**
     * expired / wrong_quantity / damaged / wrong_item
     */
    private String discrepancyType;

    /**
     * Обязателен при hasDiscrepancy=true (провалидируем в сервисе).
     */
    private String description;
}
