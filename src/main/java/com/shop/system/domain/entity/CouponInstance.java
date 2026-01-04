package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupon_instances")
public class CouponInstance {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "instance_code", nullable = false, unique = true, length = 50)
    private String instanceCode;

    @Column(name = "printed_date")
    private Instant printedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printed_by")
    private Employee printedBy;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_receipt_id")
    private SalesReceipt redeemedReceipt;
}
