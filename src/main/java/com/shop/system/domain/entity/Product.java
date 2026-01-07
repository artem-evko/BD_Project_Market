package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    @Column(name = "length", length = 20)
    private String length;

    @Column(name = "height", length = 20)
    private String height;

    @Column(name = "width", length = 20)
    private String width;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "barcode", length = 20)
    private String barcode;

    @Column(name = "additional_info", columnDefinition = "text")
    private String additionalInfo;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "archived", nullable = false)
    private Boolean archived;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductCategoryLink> categoryLinks;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (archived == null) archived = false;
    }
}
