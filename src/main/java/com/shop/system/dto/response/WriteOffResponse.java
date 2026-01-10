package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WriteOffResponse {

    private UUID id;

    private UUID productId;
    private String productName;

    private UUID batchId;

    private UUID storageZoneId;
    private String storageZoneName;

    private BigDecimal quantity;

    private String reason;   // EXPIRED/...
    private String status;   // DRAFT/...

    private LocalDate dateWrittenOff;

    private String comment;
    private String documentNumber;
    private String rejectComment;

    private String createdByEmployeeFullName;
    private String approvedByDirectorFullName;
    private Instant approvedAt;
}
