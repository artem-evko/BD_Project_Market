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
@Table(name = "employee_worktime")
public class EmployeeWorktime {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Дата смены (обычно "сегодня" или "вчера" для open)
     */
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    @Column(name = "actual_login")
    private LocalDateTime actualLogin;

    @Column(name = "actual_logout")
    private LocalDateTime actualLogout;

    /**
     * draft / confirmed / sent (по ТЗ)
     */
    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "auto_closed")
    private Boolean autoClosed;

    @Column(name = "auto_close_reason")
    private String autoCloseReason;

    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;

    @Column(name = "confirmation_deadline_at")
    private LocalDateTime confirmationDeadlineAt;

    /**
     * continue / stop / no_response
     */
    @Column(name = "confirmation_result", length = 30)
    private String confirmationResult;

    @Column(name = "confirmation_response_at")
    private LocalDateTime confirmationResponseAt;
}
