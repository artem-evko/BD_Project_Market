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
@Table(name = "shift_operations")
public class ShiftOperation {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    // shift_id -> shifts.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    // reference_id -> полиморфная ссылка (write_offs/warehouse_operations/orders)
    @Column(name = "reference_id", columnDefinition = "uuid")
    private UUID referenceId;

    @Column(name = "reference_table", nullable = false, length = 50)
    private String referenceTable;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
