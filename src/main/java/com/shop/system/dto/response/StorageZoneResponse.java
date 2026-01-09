package com.shop.system.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageZoneResponse {

    private UUID id;
    private String name;
    private String zoneType;
    private String temperatureMode;
    private BigDecimal capacity;
    private Boolean active;
}
