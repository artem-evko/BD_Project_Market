package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "price_restrictions")
public class PriceRestriction {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType; // all/product/category

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id")
    private StorageLocation storageLocation; // NULL = для всех ТТ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "max_daily_change_percent", precision = 5, scale = 2)
    private BigDecimal maxDailyChangePercent;

    @Column(name = "max_markup_percent", precision = 5, scale = 2)
    private BigDecimal maxMarkupPercent;

    @Column(name = "allow_daily_change_exception_for_auto_markdown")
    private Boolean allowDailyChangeExceptionForAutoMarkdown;

    @Column(name = "min_price", precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
