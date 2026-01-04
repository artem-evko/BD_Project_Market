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
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "weekly_hours_norm", nullable = false)
    private Integer weeklyHoursNorm;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @PrePersist
    public void prePersist() {
        if (weeklyHoursNorm == null) weeklyHoursNorm = 40;
    }
}
