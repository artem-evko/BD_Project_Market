package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    // product_id -> product.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // supply_invoice_id -> supply_invoices.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_invoice_id")
    private SupplyInvoice supplyInvoice;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "initial_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal initialQuantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
