package com.shop.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shop.system.exception.ValidationException;
import com.shop.system.service.DbSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DbSnapshotServiceImpl implements DbSnapshotService {

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // --- Публичный метод ---

    @Override
    @Transactional
    public ExportedSnapshot exportSnapshot(LocalDate snapshotDate) {
        validate(snapshotDate);

        String fileName = "db-snapshot-" + snapshotDate + ".json";
        Instant generatedAt = Instant.now();
        LocalDateTime dateEnd = snapshotDate.atTime(LocalTime.MAX);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshotDate", snapshotDate);
        payload.put("generatedAt", generatedAt);
        payload.put("sections", new LinkedHashMap<String, Object>());

        Map<String, Object> sections = castMap(payload.get("sections"));

        // --- Состав слепка по ТЗ ---
        sections.put("dictionaries", buildDictionaries());
        sections.put("prices", buildPrices(snapshotDate, dateEnd));
        sections.put("stocks", buildStocks());
        sections.put("warehouseOperations", buildWarehouseOperations(dateEnd));

        // мета/счётчики
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("fileName", fileName);
        meta.put("snapshotDate", snapshotDate.toString());
        meta.put("generatedAt", generatedAt.toString());
        meta.put("counts", buildCounts(sections));

        byte[] bytes = toJsonBytes(payload);

        // запись в exchange_log (best-effort)
        writeExchangeLogBestEffort(snapshotDate, fileName, bytes.length, true, null);

        return new ExportedSnapshot(fileName, bytes, meta);
    }

    // --- Секции ---

    private Map<String, Object> buildDictionaries() {
        Map<String, Object> dict = new LinkedHashMap<>();

        dict.put("product", safeSelectAll("product"));
        dict.put("manufacturer", safeSelectAll("manufacturer"));
        // В проекте есть сущность ProductCategory, таблица часто называется product_categories
        dict.put("categories", safeSelectAll(firstExistingTable("product_categories", "product_category", "categories")));
        dict.put("contractors", safeSelectAll(firstExistingTable("contractors", "contractor")));
        dict.put("contracts", safeSelectAll(firstExistingTable("contracts", "contract")));

        return dict;
    }

    private Object buildPrices(LocalDate snapshotDate, LocalDateTime dateEnd) {
        // По ТЗ: "цены: store_prices на дату"
        // Но в твоей схеме часто бывает price_history. Делаем best-effort:
        // 1) если есть store_prices -> фильтруем по колонке даты
        // 2) иначе если есть price_history -> фильтруем по времени <= dateEnd (если есть timestamp/date колонка)
        String storePrices = firstExistingTable("store_prices");
        if (storePrices != null) {
            return selectByDateBestEffort(storePrices, snapshotDate);
        }

        String priceHistory = firstExistingTable("price_history");
        if (priceHistory != null) {
            return selectUpToDateTimeBestEffort(priceHistory, dateEnd);
        }

        // fallback — пусто
        return List.of();
    }

    private Map<String, Object> buildStocks() {
        // По ТЗ: "остатки: batch_locations + batches"
        Map<String, Object> stocks = new LinkedHashMap<>();
        stocks.put("batches", safeSelectAll(firstExistingTable("batches", "batch")));
        stocks.put("batchLocations", safeSelectAll(firstExistingTable("batch_locations", "batch_location")));
        return stocks;
    }

    private Object buildWarehouseOperations(LocalDateTime dateEnd) {
        // По ТЗ: warehouse_operations
        String wo = firstExistingTable("warehouse_operations", "warehouse_operation");
        if (wo == null) return List.of();
        return selectUpToDateTimeBestEffort(wo, dateEnd);
    }

    // --- Best-effort DB helpers ---

    private List<Map<String, Object>> safeSelectAll(String table) {
        if (table == null) return List.of();
        if (!tableExists(table)) return List.of();
        return jdbcTemplate.queryForList("select * from " + table);
    }

    private Object selectByDateBestEffort(String table, LocalDate date) {
        if (!tableExists(table)) return List.of();

        Set<String> cols = getColumnNames(table);

        // частые варианты колонок:
        // snapshot_date / price_date / date / on_date / created_date
        String dateCol =
                pickFirst(cols, "snapshot_date", "price_date", "on_date", "date", "created_date");

        if (dateCol == null) {
            // нет даты — отдаём как есть (лучше, чем упасть)
            return safeSelectAll(table);
        }

        String sql = "select * from " + table + " where " + dateCol + " = ?";
        return jdbcTemplate.queryForList(sql, date);
    }

    private Object selectUpToDateTimeBestEffort(String table, LocalDateTime dateEnd) {
        if (!tableExists(table)) return List.of();

        Set<String> cols = getColumnNames(table);

        // частые варианты:
        // created_at / updated_at / operation_time / performed_at / changed_at / ts / timestamp
        String tsCol =
                pickFirst(cols, "operation_time", "performed_at", "changed_at", "created_at", "updated_at", "ts", "timestamp");

        if (tsCol == null) {
            // может быть просто "date"
            String dateCol = pickFirst(cols, "date", "operation_date");
            if (dateCol == null) return safeSelectAll(table);

            String sql = "select * from " + table + " where " + dateCol + " <= ?";
            return jdbcTemplate.queryForList(sql, dateEnd.toLocalDate());
        }

        String sql = "select * from " + table + " where " + tsCol + " <= ?";
        return jdbcTemplate.queryForList(sql, dateEnd);
    }

    private Map<String, Object> buildCounts(Map<String, Object> sections) {
        Map<String, Object> counts = new LinkedHashMap<>();

        Object dictObj = sections.get("dictionaries");
        if (dictObj instanceof Map<?, ?> dict) {
            Map<String, Object> dictCounts = new LinkedHashMap<>();
            for (var e : dict.entrySet()) {
                dictCounts.put(String.valueOf(e.getKey()), sizeOfList(e.getValue()));
            }
            counts.put("dictionaries", dictCounts);
        }

        counts.put("prices", sizeOfList(sections.get("prices")));
        Object stocksObj = sections.get("stocks");
        if (stocksObj instanceof Map<?, ?> st) {
            Map<String, Object> stCounts = new LinkedHashMap<>();
            for (var e : st.entrySet()) {
                stCounts.put(String.valueOf(e.getKey()), sizeOfList(e.getValue()));
            }
            counts.put("stocks", stCounts);
        }

        counts.put("warehouseOperations", sizeOfList(sections.get("warehouseOperations")));
        return counts;
    }

    private int sizeOfList(Object x) {
        if (x instanceof Collection<?> c) return c.size();
        return 0;
    }

    private byte[] toJsonBytes(Object obj) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось сформировать JSON слепка: " + e.getMessage(), e);
        }
    }

    // --- Exchange log (best-effort insert) ---

    private void writeExchangeLogBestEffort(LocalDate snapshotDate,
                                            String fileName,
                                            int payloadSizeBytes,
                                            boolean success,
                                            String errorMessage) {
        String table = firstExistingTable("exchange_log");
        if (table == null) return;

        try {
            Set<String> cols = getColumnNames(table);

            Map<String, Object> values = new LinkedHashMap<>();

            // id (uuid)
            if (cols.contains("id")) values.put("id", UUID.randomUUID());

            // created_at
            if (cols.contains("created_at")) values.put("created_at", LocalDateTime.now());

            // status
            if (cols.contains("status")) values.put("status", success ? "SUCCESS" : "ERROR");

            // operation
            if (cols.contains("operation")) values.put("operation", "DB_SNAPSHOT_EXPORT");

            // ВАЖНО: exchange_log требует entity_name NOT NULL
            if (cols.contains("entity_name")) values.put("entity_name", "DB_SNAPSHOT");
            // иногда бывает entityType/entity
            if (cols.contains("entity_type")) values.put("entity_type", "DB_SNAPSHOT");

            // entity_id (часто nullable, но если есть — можно положить uuid слепка)
            if (cols.contains("entity_id")) values.put("entity_id", UUID.randomUUID());

            // message
            if (cols.contains("message")) {
                values.put("message", success
                        ? ("DB Snapshot exported for " + snapshotDate)
                        : ("DB Snapshot export failed: " + errorMessage));
            }

            // file_name
            if (cols.contains("file_name")) values.put("file_name", fileName);

            // payload size
            if (cols.contains("payload_size")) values.put("payload_size", payloadSizeBytes);

            // snapshot_date
            if (cols.contains("snapshot_date")) values.put("snapshot_date", snapshotDate);

            // payload (если есть) — положим компактную мету (обычно text/jsonb)
            if (cols.contains("payload")) {
                String metaJson = "{\"fileName\":\"" + fileName + "\",\"snapshotDate\":\"" + snapshotDate + "\",\"bytes\":" + payloadSizeBytes + "}";
                values.put("payload", metaJson);
            }

            if (values.isEmpty()) return;

            StringBuilder sql = new StringBuilder("insert into " + table + " (");
            StringBuilder ph  = new StringBuilder(" values (");

            List<Object> args = new ArrayList<>();
            int i = 0;
            for (var e : values.entrySet()) {
                if (i++ > 0) { sql.append(", "); ph.append(", "); }
                sql.append(e.getKey());
                ph.append("?");
                args.add(e.getValue());
            }
            sql.append(")");
            ph.append(")");
            sql.append(ph);

            jdbcTemplate.update(sql.toString(), args.toArray());
        } catch (Exception ignored) {
            // реально best-effort: экспорт не должен падать из-за exchange_log
        }
    }


    // --- Schema introspection helpers ---

    private boolean tableExists(String table) {
        if (table == null) return false;
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_name = ?",
                    Integer.class,
                    table.toLowerCase()
            );
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            // если нет доступа к information_schema — попробуем простую проверку
            try {
                jdbcTemplate.queryForObject("select 1 from " + table + " limit 1", Integer.class);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private Set<String> getColumnNames(String table) {
        try {
            return new HashSet<>(jdbcTemplate.query(
                    "select column_name from information_schema.columns where table_name = ?",
                    (rs, rowNum) -> rs.getString("column_name").toLowerCase(),
                    table.toLowerCase()
            ));
        } catch (Exception e) {
            // fallback: пусто
            return Set.of();
        }
    }

    private String firstExistingTable(String... candidates) {
        for (String t : candidates) {
            if (t != null && tableExists(t)) return t;
        }
        return null;
    }

    private String pickFirst(Set<String> cols, String... names) {
        for (String n : names) {
            if (n != null && cols.contains(n.toLowerCase())) return n.toLowerCase();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object x) {
        return (Map<String, Object>) x;
    }

    // --- Validation ---

    private void validate(LocalDate snapshotDate) {
        if (snapshotDate == null) {
            throw new ValidationException("snapshotDate обязателен");
        }
        if (snapshotDate.isAfter(LocalDate.now())) {
            throw new ValidationException("snapshotDate не может быть в будущем");
        }
    }
}
