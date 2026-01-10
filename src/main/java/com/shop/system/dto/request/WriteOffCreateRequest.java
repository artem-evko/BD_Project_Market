package com.shop.system.dto.request;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WriteOffCreateRequest {

    private UUID productId;
    private UUID batchId;
    private UUID storageZoneId;
    private BigDecimal quantity;
    private String reason;
    private String comment;
    private String documentNumber;
    private boolean submit;
}
