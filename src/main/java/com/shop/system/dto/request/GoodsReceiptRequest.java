package com.shop.system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptRequest {

    /**
     * Дата фактической поставки (шапка накладной).
     * Может быть null до подтверждения, но при confirm — обязательна.
     */
    private LocalDate actualDate;

    /**
     * Куда фактически разместили (зона хранения).
     * Может быть null до подтверждения, но при confirm — обязательна.
     */
    private UUID receivedZoneId;

    @NotNull
    @Valid
    private List<GoodsReceiptItemFactRequest> items;
}
