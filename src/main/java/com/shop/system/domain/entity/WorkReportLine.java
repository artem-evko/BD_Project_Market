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
@Table(name = "work_report_lines")
public class WorkReportLine {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private WorkReportHeader report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "computed_hours", precision = 5, scale = 2)
    private BigDecimal computedHours;

    @Column(name = "norm_hours", precision = 5, scale = 2)
    private BigDecimal normHours;

    @Column(name = "delta_hours", precision = 5, scale = 2)
    private BigDecimal deltaHours;

    @Column(name = "director_override_delta", precision = 5, scale = 2)
    private BigDecimal directorOverrideDelta;

    @Column(name = "override_reason", columnDefinition = "text")
    private String overrideReason;
}
