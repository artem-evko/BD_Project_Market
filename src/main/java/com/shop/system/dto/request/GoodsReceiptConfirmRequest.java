package com.shop.system.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptConfirmRequest {

    /**
     * Можно передать, чтобы при confirm проставить в накладную (если не заполнено).
     */
    private LocalDate actualDate;

    /**
     * Можно передать, чтобы при confirm проставить в накладную (если не заполнено).
     */
    private UUID receivedZoneId;
}
