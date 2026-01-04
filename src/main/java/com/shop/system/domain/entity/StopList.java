package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stop_list")
public class StopList {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private PriceChangeTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_item_id")
    private PriceListItem priceListItem;

    @Column(name = "candidate_price", precision = 10, scale = 2)
    private BigDecimal candidatePrice;

    @Column(name = "prev_price", precision = 10, scale = 2)
    private BigDecimal prevPrice;

    @Column(name = "input_price", precision = 10, scale = 2)
    private BigDecimal inputPrice;

    // JSONB — пока строкой (позже можно сделать Map + JsonType)
    @Column(name = "violations", columnDefinition = "jsonb")
    private String violations;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "status", length = 20)
    private String status; // pending/sent/accepted/rejected

    @Column(name = "sent_to_hq_at")
    private LocalDateTime sentToHqAt;

    @Column(name = "hq_response_at")
    private LocalDateTime hqResponseAt;

    @Column(name = "hq_comment", columnDefinition = "text")
    private String hqComment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
