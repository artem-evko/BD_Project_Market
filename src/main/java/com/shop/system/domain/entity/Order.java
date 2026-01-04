package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "order_date", nullable = false)
    private Instant orderDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "total_sum", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;
}
