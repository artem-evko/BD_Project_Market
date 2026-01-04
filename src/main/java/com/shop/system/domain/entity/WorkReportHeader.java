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
@Table(name = "work_report_headers")
public class WorkReportHeader {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_location_id", nullable = false)
    private StorageLocation storageLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Employee director;

    @Column(name = "status", length = 20)
    private String status; // draft/confirmed/sent

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "sent_to_hq_at")
    private LocalDateTime sentToHqAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
