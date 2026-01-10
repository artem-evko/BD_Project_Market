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
@Table(
        name = "supply_invoice_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supply_invoice_items_invoice_line",
                        columnNames = {"supply_invoice_id", "line_no"})
        }
)
public class SupplyInvoiceItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_invoice_id", nullable = false)
    private SupplyInvoice supplyInvoice;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_expected", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityExpected;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "quantity_actual", precision = 10, scale = 2)
    private BigDecimal quantityActual;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "line_status", nullable = false, length = 30)
    private String lineStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discrepancy_request_id")
    private DiscrepancyRequest discrepancyRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fact_entered_by")
    private Employee factEnteredBy;

    @Column(name = "fact_entered_at")
    private Instant factEnteredAt;

    // --- lifecycle hooks ---
    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (lineStatus == null) lineStatus = "pending";
    }
}
