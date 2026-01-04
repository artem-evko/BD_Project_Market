package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /**
     * В БД: JSONB NOT NULL.
     * Для простоты на старте храним как String (JSON строкой).
     * Потом можно заменить на Map<String, Object> или отдельный класс.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "passport_data", nullable = false, columnDefinition = "jsonb")
    private String passportData;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "employment_date")
    private LocalDate employmentDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "employment_status", length = 100)
    private String employmentStatus;

    @Column(name = "work_phone", length = 20)
    private String workPhone;

    @Column(name = "personal_phone", length = 20)
    private String personalPhone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToOne(mappedBy = "employee", fetch = FetchType.LAZY)
    private UserAccount userAccount;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (employmentStatus == null) employmentStatus = "active";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
