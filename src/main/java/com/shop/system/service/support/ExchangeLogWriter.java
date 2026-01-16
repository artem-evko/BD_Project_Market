package com.shop.system.service.support;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExchangeLogWriter {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(String entityName, UUID entityId, String operation, String direction, String message) {
        insert(entityName, entityId, operation, direction, "success", message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void error(String entityName, UUID entityId, String operation, String direction, String message) {
        insert(entityName, entityId, operation, direction, "error", message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void error(String entityName, UUID entityId, String operation, String direction, Exception e) {
        insert(entityName, entityId, operation, direction, "error", safe(e.getMessage()));
    }

    private void insert(String entityName, UUID entityId, String operation, String direction, String status, String message) {
        jdbcTemplate.update("""
                INSERT INTO exchange_log (id, entity_name, entity_id, operation, direction, status, message, "timestamp")
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                entityName,
                entityId,
                operation,
                direction,
                status,
                message,
                OffsetDateTime.now()
        );
    }

    private String safe(String s) {
        if (s == null || s.isBlank()) return "error";
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
