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
@Table(name = "label_print_queue")
public class LabelPrintQueue {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_item_id")
    private PriceListItem priceListItem;

    @Column(name = "label_type", length = 20)
    private String labelType; // white / yellow / promotion

    @Column(name = "print_status", length = 20)
    private String printStatus; // pending / printed

    @Column(name = "print_date")
    private Instant printDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printed_by")
    private Employee printedBy;
}
