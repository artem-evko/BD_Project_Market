package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "scheduled_start")
    private LocalTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalTime scheduledEnd;

    @Column(name = "actual_login")
    private LocalDateTime actualLogin;

    @Column(name = "actual_logout")
    private LocalDateTime actualLogout;

    @Column(name = "status", length = 50)
    private String status; // норма/переработка/недоработка/...

    @Column(name = "auto_closed")
    private Boolean autoClosed;

    @Column(name = "auto_close_reason", length = 100)
    private String autoCloseReason;

    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;

    @Column(name = "confirmation_deadline_at")
    private LocalDateTime confirmationDeadlineAt;

    @Column(name = "confirmation_response_at")
    private LocalDateTime confirmationResponseAt;

    @Column(name = "confirmation_result", length = 20)
    private String confirmationResult; // continue/stop/no_response

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;
}
