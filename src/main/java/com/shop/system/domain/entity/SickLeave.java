package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sick_leaves")
public class SickLeave {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_employee_id")
    private Employee approvedByEmployee;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "date_start")
    private LocalDate dateStart;

    @Column(name = "date_end")
    private LocalDate dateEnd;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "doc_required")
    private Boolean docRequired;

    @Column(name = "doc_due_date")
    private LocalDate docDueDate;

    @Column(name = "doc_received_at")
    private LocalDateTime docReceivedAt;

    @Column(name = "doc_status", length = 20)
    private String docStatus; // pending/received/overdue

    @Column(name = "reject_comment", columnDefinition = "text")
    private String rejectComment;
}
