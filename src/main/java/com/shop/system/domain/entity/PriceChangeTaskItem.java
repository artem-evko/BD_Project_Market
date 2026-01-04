package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "price_change_task_items")
public class PriceChangeTaskItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private PriceChangeTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_price_list_item_id")
    private PriceListItem selectedPriceListItem;

    @Column(name = "selected_final_price", precision = 10, scale = 2)
    private BigDecimal selectedFinalPrice;

    @Column(name = "regular_price", precision = 10, scale = 2)
    private BigDecimal regularPrice;

    @Column(name = "label_type", length = 20)
    private String labelType; // white / yellow / promotion

    @Column(name = "restriction_applied")
    private Boolean restrictionApplied;

    @Column(name = "restriction_reason", length = 50)
    private String restrictionReason; // daily_change_limit / markup_cap / ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_list_id")
    private StopList stopList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
