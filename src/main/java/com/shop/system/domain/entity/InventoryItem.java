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
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    // FK -> inventory.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    // FK -> product.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "expected_qty", precision = 10, scale = 2)
    private BigDecimal expectedQty;

    @Column(name = "actual_qty", precision = 10, scale = 2)
    private BigDecimal actualQty;

    // FK -> batches.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    // FK -> discrepancy_requests.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discrepancy_request_id")
    private DiscrepancyRequest discrepancyRequest;
}
