package com.shop.system.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exchange_log")
public class ExchangeLog {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation; // insert / update / delete

    @Column(name = "direction", nullable = false, length = 10)
    private String direction; // upload / download

    @Column(name = "status", nullable = false, length = 20)
    private String status; // success / error

    @Column(name = "message")
    private String message;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
