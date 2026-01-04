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
@Table(name = "discrepancy_requests")
public class DiscrepancyRequest {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    // supply_invoice_id -> supply_invoices.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_invoice_id", nullable = false)
    private SupplyInvoice supplyInvoice;

    // supply_invoice_item_id -> supply_invoice_items.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_invoice_item_id", nullable = false)
    private SupplyInvoiceItem supplyInvoiceItem;

    // product_id -> product.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // batch_id -> batches.id (может быть NULL до финального подтверждения)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "discrepancy_type", nullable = false, length = 50)
    private String discrepancyType; // expired / wrong_quantity / damaged / wrong_item

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "quantity_expected", precision = 10, scale = 2)
    private BigDecimal quantityExpected;

    @Column(name = "quantity_actual", precision = 10, scale = 2)
    private BigDecimal quantityActual;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    // created_by_employee_id -> employees.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id", nullable = false)
    private Employee createdByEmployee;

    // decision_by_employee_id -> employees.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_by_employee_id")
    private Employee decisionByEmployee;

    @Column(name = "decision_at")
    private Instant decisionAt;

    @Column(name = "decision_comment", columnDefinition = "text")
    private String decisionComment;

    @Column(name = "hq_decision_at")
    private Instant hqDecisionAt;

    @Column(name = "hq_decision_comment", columnDefinition = "text")
    private String hqDecisionComment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
