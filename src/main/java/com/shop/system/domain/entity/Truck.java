package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "plate_number", nullable = false, length = 10)
    private String plateNumber;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "capacity_kg")
    private Integer capacityKg;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "note", columnDefinition = "text")
    private String note;
}
