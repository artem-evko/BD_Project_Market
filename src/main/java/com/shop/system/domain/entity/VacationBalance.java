package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vacation_balance")
public class VacationBalance {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "carried_over", precision = 5, scale = 2)
    private BigDecimal carriedOver;

    @Column(name = "accrued", precision = 5, scale = 2)
    private BigDecimal accrued;

    @Column(name = "used", precision = 5, scale = 2)
    private BigDecimal used;
}
