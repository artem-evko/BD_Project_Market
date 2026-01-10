package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "supply_invoices")
public class SupplyInvoice {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_zone_id")
    private StorageZone receivedZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storekeeper_id")
    private Employee storekeeper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandiser_id")
    private Employee merchandiser;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "supplyInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplyInvoiceItem> items = new ArrayList<>();

    // --- lifecycle hooks ---
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = "expected";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}

