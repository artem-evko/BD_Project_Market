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
@Table(name = "warehouse_operations")
public class WarehouseOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // RECEIPT, SHIPMENT, TRANSFER, WRITE_OFF, INVENTORY_ADJUSTMENT

    // FK -> product.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // FK -> storage_zones.id (откуда)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_zone_id")
    private StorageZone fromZone;

    // FK -> storage_zones.id (куда)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_zone_id")
    private StorageZone toZone;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "operation_date", nullable = false)
    private Instant operationDate;

    // FK -> employees.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    // FK -> batches.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "source_document_type", length = 50)
    private String sourceDocumentType;

    @Column(name = "source_document_id", columnDefinition = "uuid")
    private UUID sourceDocumentId;
}
