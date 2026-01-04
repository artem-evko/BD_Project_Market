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
                @UniqueConstraint(name = "uk_supply_invoice_items_invoice_line", columnNames = {"supply_invoice_id", "line_no"})
        }
)
public class SupplyInvoiceItem {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    // supply_invoice_id -> supply_invoices.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_invoice_id", nullable = false)
    private SupplyInvoice supplyInvoice;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    // product_id -> product.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ожидаемые данные
    @Column(name = "quantity_expected", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantityExpected;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // фактические данные (могут быть NULL до приемки)
    @Column(name = "quantity_actual", precision = 10, scale = 2)
    private BigDecimal quantityActual;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "line_status", nullable = false, length = 30)
    private String lineStatus; // pending / ok / discrepancy_pending / accepted / rejected (как у тебя в скрине)

    // discrepancy_request_id -> discrepancy_requests.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discrepancy_request_id")
    private DiscrepancyRequest discrepancyRequest;

    // batch_id -> batches.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    // fact_entered_by -> employees.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fact_entered_by")
    private Employee factEnteredBy;

    @Column(name = "fact_entered_at")
    private Instant factEnteredAt;
}
