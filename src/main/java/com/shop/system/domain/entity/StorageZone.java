package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "storage_zones")
public class StorageZone {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    // storage_location_id -> storage_locations.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "zone_type", nullable = false, length = 50)
    private String zoneType; // refrigerator/freezer/shelf/backroom/display

    @Column(name = "temperature_mode", nullable = false, length = 50)
    private String temperatureMode; // normal/cool/frozen

    @Column(name = "capacity", precision = 10, scale = 2)
    private BigDecimal capacity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
