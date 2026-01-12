package com.shop.system.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeLogListItemResponse {

    private UUID id;
    private Instant timestamp;

    private String entityName;
    private UUID entityId;

    private String operation;  // insert / update / delete
    private String direction;  // upload / download
    private String status;     // success / error

    private String message;
}
